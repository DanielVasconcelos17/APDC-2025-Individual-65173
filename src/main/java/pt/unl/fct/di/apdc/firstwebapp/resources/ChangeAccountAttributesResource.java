package pt.unl.fct.di.apdc.firstwebapp.resources;


import com.google.cloud.datastore.*;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.gson.Gson;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import pt.unl.fct.di.apdc.firstwebapp.authentication.TokenValidator;
import pt.unl.fct.di.apdc.firstwebapp.response.UserFullListing;
import pt.unl.fct.di.apdc.firstwebapp.types.ProfileState;
import pt.unl.fct.di.apdc.firstwebapp.types.ProfileType;
import pt.unl.fct.di.apdc.firstwebapp.types.Role;
import pt.unl.fct.di.apdc.firstwebapp.types.UserDSFields;
import pt.unl.fct.di.apdc.firstwebapp.util.ChangeAttributesData;


import java.util.logging.Logger;

@Path("/changeAttributes")
@Produces(MediaType.APPLICATION_JSON)
public class ChangeAccountAttributesResource {

    private static final String TOKEN_ROLE = "token_role";
    private static final String TOKEN_USERNAME = "token_username";


    private static final Logger LOG = Logger.getLogger(ChangeAccountAttributesResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
    private static final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind("Token");

    private final Gson g = new Gson();


    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeAttributes(ChangeAttributesData data) {

        // Buscar o tokenID associado ao requesterUsername
        Query<Entity> tokenQuery = Query.newEntityQueryBuilder()
                .setKind("Token")
                .setFilter(PropertyFilter.eq(TOKEN_USERNAME, data.requesterUsername))
                .build();
        QueryResults<Entity> allTokens = datastore.run(tokenQuery);

        if (!allTokens.hasNext()) {
            return Response.status(Status.FORBIDDEN)
                    .entity(g.toJson("No active token found for requester."))
                    .build();
        }

        Entity tokenEntity = allTokens.next();
        String tokenID = tokenEntity.getKey().getName(); // Obtém o ID do token

        // Verificar se o token é válido
        if (!TokenValidator.isValidToken(tokenID)) {
            return Response.status(Status.FORBIDDEN).entity("Invalid or expired token.").build();
        }
        Transaction txn = datastore.newTransaction();
        try {
            //Key tokenKey = tokenKeyFactory.newKey(tokenID);
            //tokenEntity = txn.get(tokenKey);  CUIDADO TODO

            // Obter o token e o role do user que está a fazer a alteraçao
            String requesterRole = tokenEntity.getString(TOKEN_ROLE);
            String requesterUsername = tokenEntity.getString(TOKEN_USERNAME);

            //Para verificar o state do user que está a fazer o pedido
            Key requesterKey = userKeyFactory.newKey(requesterUsername);
            Entity requesterEntity = txn.get(requesterKey);
            String requesterState = requesterEntity.getString(UserDSFields.USER_STATE.toString());

            Key targetKey = userKeyFactory.newKey(data.targetUsername);
            Entity targetEntity = txn.get(targetKey);

            if (targetEntity == null)
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(g.toJson("User not found.")).build();

            String targetRole = targetEntity.getString(UserDSFields.USER_ROLE.toString());
            boolean isSelfUpdate = requesterUsername.equals(data.targetUsername);

            // Verificar permissões
            if (!hasPermissionToModify(requesterRole, targetRole, isSelfUpdate, requesterState))
                return Response.status(Status.FORBIDDEN)
                        .entity(g.toJson("Insufficient permissions.")).build();

            Entity.Builder updatedUser = Entity.newBuilder(targetEntity);
            // Atualiza conforme o role do user que faz pedido e do alvo
            if (isSelfUpdate && requesterRole.equals(Role.ENDUSER.getType()))
                updateEndUserFields(data, updatedUser);
            if (requesterRole.equals(Role.BACKOFFICE.getType()))
                updateBackOfficeFields(data, updatedUser);
            if (requesterRole.equals(Role.ADMIN.getType()))
                updateAdminFields(data, updatedUser, targetEntity, txn);

            txn.put(updatedUser.build());
            txn.commit();
            //Para visualização dos atributos
            UserFullListing view = new UserFullListing(updatedUser.build());
            return Response.ok(g.toJson(view)).build();
        } catch (Exception e) {
            txn.rollback();
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }finally {
            if (txn.isActive())
                txn.rollback();
        }
    }

    private void updateCommonFields(ChangeAttributesData data,
                                    Entity.Builder updatedUser) {
        if (data.citizenCard != null)
            updatedUser.set(UserDSFields.USER_CC.toString(), data.citizenCard);
        if (data.phone != null)
            updatedUser.set(UserDSFields.USER_PHONE.toString(), data.phone);
        if (data.profile != null) {
            ProfileType profileType = ProfileType.fromString(data.profile);
            if (profileType != null)
                updatedUser.set(UserDSFields.USER_PROFILE.toString(), profileType.getType());
        }
        if (data.nif != null)
            updatedUser.set(UserDSFields.USER_NIF.toString(), data.nif);
        if (data.employer != null)
            updatedUser.set(UserDSFields.USER_EMPLOYER.toString(), data.employer);
        if (data.employerNif != null)
            updatedUser.set(UserDSFields.USER_EMP_NIF.toString(), data.employerNif);
        if (data.job != null)
            updatedUser.set(UserDSFields.USER_JOB.toString(), data.job);
        if (data.address != null)
            updatedUser.set(UserDSFields.USER_ADDRESS.toString(), data.address);
    }

    // ENDUSER pode alterar todos os SEUS atributos exceto
    // username, email, nome, role e state
    private void updateEndUserFields(ChangeAttributesData data,
                                     Entity.Builder updatedUser) {
        this.updateCommonFields(data, updatedUser);
    }

    // BACKOFFICE pode modificar todos atributos exceto username e email
    // Pode modificar role apenas entre ENDUSER e PARTNER
    private void updateBackOfficeFields(ChangeAttributesData data, Entity.Builder updatedUser) {
        if (data.name != null)
            updatedUser.set(UserDSFields.USER_NAME.toString(), data.name);
        this.updateCommonFields(data, updatedUser);
    }

    private void updateAdminFields(ChangeAttributesData data, Entity.Builder updatedUser,
                                   Entity originalUser, Transaction txn) {

        String oldUsername = originalUser.getKey().getName();
        boolean usernameChanged = data.targetUpdtUsername != null
                && !data.targetUpdtUsername.equals(oldUsername);

        if (usernameChanged) {
            Key newKey = userKeyFactory.newKey(data.targetUpdtUsername);
            updatedUser.setKey(newKey);

            originalUser.getProperties().forEach((key, value) -> {
                if (!key.equals("__key__")) {
                    updatedUser.set(key, value);
                }
            });

            updateUserTokens(txn, oldUsername, data.targetUpdtUsername);
            txn.delete(originalUser.getKey());
        }
        if (data.email != null)
            updatedUser.set(UserDSFields.USER_EMAIL.toString(), data.email);
        if (data.name != null)
            updatedUser.set(UserDSFields.USER_NAME.toString(), data.name);

        this.updateCommonFields(data, updatedUser);

        if (data.role != null)
            updatedUser.set(UserDSFields.USER_ROLE.toString(), data.role);
        if (data.state != null)
            updatedUser.set(UserDSFields.USER_STATE.toString(), data.state);
    }

    private void updateUserTokens(Transaction txn, String oldUsername, String newUsername) {
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("Token")
                .setFilter(PropertyFilter.eq(TOKEN_USERNAME, oldUsername))
                .build();

        QueryResults<Entity> tokens = txn.run(query);

        tokens.forEachRemaining(token -> {
            Entity updatedToken = Entity.newBuilder(token)
                    .set(TOKEN_USERNAME, newUsername)
                    .build();
            txn.update(updatedToken);
        });
    }

    private boolean hasPermissionToModify(String requesterRole, String targetRole,
                                          boolean isSelfUpdate, String requesterState) {
        Role requester = Role.valueOf(requesterRole);
        switch (requester) {
            case Role.ADMIN:
                return true; // ADMIN pode alterar qualquer conta
            case Role.BACKOFFICE:
                // BACKOFFICE pode alterar própria conta ou contas ENDUSER/PARTNER
                return !isSelfUpdate && requesterState.equals(ProfileState.ACTIVATE.getType())
                        && (targetRole.equals(Role.ENDUSER.getType()) ||
                        targetRole.equals(Role.PARTNER.getType()));
            case Role.ENDUSER:
                // ENDUSER só pode modificar a própria conta
                return isSelfUpdate
                        && requesterState.equals(ProfileState.ACTIVATE.getType());
            case Role.PARTNER:
                // PARTNER só pode alterar a própria conta
                return isSelfUpdate
                        && requesterState.equals(ProfileState.ACTIVATE.getType());
            default:
                return false;
        }
    }
}
