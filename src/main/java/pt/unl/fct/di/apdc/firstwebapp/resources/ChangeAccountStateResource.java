package pt.unl.fct.di.apdc.firstwebapp.resources;

import com.google.cloud.datastore.*;
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
import pt.unl.fct.di.apdc.firstwebapp.types.Role;
import pt.unl.fct.di.apdc.firstwebapp.types.UserDSFields;
import pt.unl.fct.di.apdc.firstwebapp.util.ChangeAccStateData;

import java.util.logging.Logger;

@Path("/changeState")
@Produces(MediaType.APPLICATION_JSON)
public class ChangeAccountStateResource {


    private static final String TOKEN_ROLE = "token_role";
    private static final String TOKEN_USERNAME = "token_username";


    private static final Logger LOG = Logger.getLogger(ChangeAccountStateResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    private final Gson g = new Gson();

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeAccState(ChangeAccStateData data){
        LOG.fine("Attempt to change user state: " + data.targetUsername);

        // Buscar o tokenID associado ao requesterUsername
        Query<Entity> tokenQuery = Query.newEntityQueryBuilder()
                .setKind("Token")
                .setFilter(StructuredQuery.PropertyFilter.eq(TOKEN_USERNAME, data.requesterUsername))
                .build();
        QueryResults<Entity> allTokens = datastore.run(tokenQuery);

        if (!allTokens.hasNext()) {
            return Response.status(Status.FORBIDDEN)
                    .entity(g.toJson("No active token found for requester."))
                    .build();
        }

        Entity tokenEntity = allTokens.next();
        String tokenID = tokenEntity.getKey().getName(); // Obtém o ID do token

        // Validação do token
        if (!TokenValidator.isValidToken(tokenID))
            return Response.status(Status.FORBIDDEN)
                    .entity(g.toJson("Invalid or expired token.")).build();


        //Validação do estado a alterar
        if(!ChangeAccStateData.isValidState(data.newState))
            return Response.status(Status.FORBIDDEN)
                    .entity(g.toJson("Not a valid state, must be " +
                            "ACTIVATE, SUSPENDED or DEACTIVATE."))
                    .build();

        String requesterRole = tokenEntity.getString(TOKEN_ROLE);

        // Verificação de permissão da role para alterar state
        if (requesterRole.equals(Role.ENDUSER.getType())
                || requesterRole.equals(Role.PARTNER.getType())) {
            return Response.status(Status.FORBIDDEN)
                    .entity(g.toJson(requesterRole + " is not allowed to change account state."))
                    .build();
        }

        if(requesterRole.equals(Role.BACKOFFICE.getType())
            && data.newState.equals(ProfileState.SUSPENDED.getType())){
            return Response.status(Status.FORBIDDEN)
                    .entity(g.toJson("BACKOFFICE can only ACTIVATE/DEACTIVATE" +
                            " accounts (not SUSPEND).")).build();
        }

        // Verificação da existencia do utilizador alvo
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.targetUsername);
        Entity targetEntity = datastore.get(userKey);

        if(targetEntity == null)
            return Response.status(Status.NOT_FOUND)
                    .entity(g.toJson("Target user not found.")).build();

        // Em caso de sucesso guardar na datastore a atualização
        Transaction txn = datastore.newTransaction();
        try {
            Entity updateUser = Entity.newBuilder(targetEntity)
                    .set(UserDSFields.USER_STATE.toString(), data.newState)
                    .build();
            txn.put(updateUser);
            txn.commit();

            UserFullListing view = new UserFullListing(updateUser);
            return Response.ok(g.toJson(view)).build();
        }catch (Exception e){
            txn.rollback();
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }finally {
            if (txn.isActive())
                txn.rollback();
        }
    }
}
