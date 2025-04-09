package pt.unl.fct.di.apdc.firstwebapp.resources;

import com.google.cloud.datastore.*;
import com.google.gson.Gson;
import jakarta.ws.rs.core.Response.Status;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.apdc.firstwebapp.authentication.TokenValidator;
import pt.unl.fct.di.apdc.firstwebapp.response.TokenValidationResult;
import pt.unl.fct.di.apdc.firstwebapp.response.UserFullListing;
import pt.unl.fct.di.apdc.firstwebapp.enums.Role;
import pt.unl.fct.di.apdc.firstwebapp.enums.UserDSFields;
import pt.unl.fct.di.apdc.firstwebapp.util.ChangeRoleData;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;


@Path("/changeRole")
@Produces(MediaType.APPLICATION_JSON)
public class ChangeRoleResource {


    private static final String TOKEN_ROLE = "token_role";
    private static final String TOKEN_USERNAME = "token_username";


    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final Gson g = new Gson();

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeRole(ChangeRoleData data) {

        Transaction txn = datastore.newTransaction();
        try {
            // Buscar o tokenID associado ao requesterUsername
            TokenValidationResult validation =
                    TokenValidator.validateToken(txn, datastore, data.requesterUsername);
            if (validation.errorResponse != null) {
                txn.rollback();
                return validation.errorResponse;
            }

            // Obter o role do user que está a fazer o pedido
            String requesterRole = validation.tokenEntity.getString(TOKEN_ROLE);

            if (!ChangeRoleData.isValidRole(requesterRole)
                    || !ChangeRoleData.isValidRole(data.newRole))
                return Response.status(Status.FORBIDDEN)
                        .entity(g.toJson("Not valid role has to be " +
                                "one of these (ENDUSER, PARTNER, ADMIN, BACKOFFICE)"))
                        .build();

            // Verificar se o o user target existe
            Key targetKey = datastore.newKeyFactory().setKind("User").newKey(data.targetUsername);
            Entity targetEntity = datastore.get(targetKey);

            if (targetEntity == null)
                return Response.status(Status.NOT_FOUND)
                        .entity(g.toJson("Target user not found."))
                        .build();

            // Obter a role do user target
            String targetUserRole = targetEntity.getString(UserDSFields.USER_ROLE.toString());

            // Verificar permissoes
            if (requesterRole.equals(Role.ENDUSER.getType()))
                return Response.status(Status.FORBIDDEN)
                        .entity(g.toJson("ENDUSER cannot change roles."))
                        .build();

            // Verifica se o user que faz o pedido de mudança é BACKOFFICE
            if (requesterRole.equals(Role.BACKOFFICE.getType())) {
                // Verifica se o user e o role a que vai ser
                // alterado se são ENDUSER ou PARTNER
                if (!targetUserRole.equals(Role.PARTNER.getType())
                        && !targetUserRole.equals(Role.ENDUSER.getType())) {
                    return Response.status(Status.FORBIDDEN)
                            .entity(g.toJson("BACKOFFICE can only change" +
                                    " ENDUSER to PARTNER and vice-versa."))
                            .build();
                }
                if (!data.newRole.equals(Role.PARTNER.getType())
                        && !data.newRole.equals(Role.ENDUSER.getType())) {
                    return Response.status(Status.FORBIDDEN)
                            .entity(g.toJson("BACKOFFICE can only assign ENDUSER or PARTNER roles."))
                            .build();
                }
            }

            Entity updateUser = Entity.newBuilder(targetEntity)
                    .set(UserDSFields.USER_ROLE.toString()
                            , data.newRole).build();
            txn.put(updateUser);

            // Atualizar todos os tokens associados ao utilizador
            Query<Entity> query = Query.newEntityQueryBuilder()
                    .setKind("Token")
                    .setFilter(PropertyFilter.eq(TOKEN_USERNAME, data.targetUsername))
                    .build();
            QueryResults<Entity> tokens = txn.run(query);

            tokens.forEachRemaining(token -> {
                Entity updatedToken = Entity.newBuilder(token)
                        .set(TOKEN_ROLE, data.newRole)
                        .build();
                txn.put(updatedToken);
            });

            txn.commit();
            UserFullListing view = new UserFullListing(updateUser);
            return Response.ok(g.toJson(view))
                    .build();
        } catch (Exception e) {
            txn.rollback();
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (txn.isActive())
                txn.rollback();
        }
    }
}
