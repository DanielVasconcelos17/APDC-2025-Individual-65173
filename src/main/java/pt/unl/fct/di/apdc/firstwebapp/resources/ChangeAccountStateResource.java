package pt.unl.fct.di.apdc.firstwebapp.resources;

import com.google.cloud.datastore.*;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.apdc.firstwebapp.authentication.TokenValidator;
import pt.unl.fct.di.apdc.firstwebapp.types.ProfileState;
import pt.unl.fct.di.apdc.firstwebapp.types.ProfileType;
import pt.unl.fct.di.apdc.firstwebapp.types.Role;
import pt.unl.fct.di.apdc.firstwebapp.util.ChangeAccStateData;

import java.util.logging.Logger;

@Path("/changeState")
@Produces(MediaType.APPLICATION_JSON)
public class ChangeAccountStateResource {

    private static final String USER_STATE = "user_state";

    private static final String TOKEN_ROLE = "token_role";

    private static final Logger LOG = Logger.getLogger(ChangeAccountStateResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeAccState(ChangeAccStateData data){
        LOG.fine("Attempt to change user state: " + data.targetUsername);

        // Validação do token
        if (!TokenValidator.isValidToken(data.tokenID))
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("Invalid or expired token.").build();


        //Validação do estado a alterar
        if(!ChangeAccStateData.isValidState(data.newState))
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("Not a valid state, must be ACTIVATE, SUSPENDED or DEACTIVATE.")
                    .build();

        Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(data.tokenID);
        Entity tokenEntity = datastore.get(tokenKey);
        String requesterRole = tokenEntity.getString(TOKEN_ROLE);

        // Verificação de permissão da role para alterar state
        if (requesterRole.equals(Role.ENDUSER.getType())
                || requesterRole.equals(Role.PARTNER.getType())) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(requesterRole + " is not allowed to change account state.")
                    .build();
        }

        if(requesterRole.equals(Role.BACKOFFICE.getType())
            && data.newState.equals(ProfileState.SUSPENDED.getType())){
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("BACKOFFICE can only ACTIVATE/DEACTIVATE" +
                            " accounts (not SUSPEND).").build();
        }

        // Verificação da existencia do utilizador alvo
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.targetUsername);
        Entity targetEntity = datastore.get(userKey);

        if(targetEntity == null)
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Target user not found.").build();

        // Em caso de sucesso guardar na datastore a atualização
        Transaction txn = datastore.newTransaction();
        try {
            Entity updateUser = Entity.newBuilder(targetEntity)
                    .set(USER_STATE, data.newState)
                    .build();
            txn.put(updateUser);
            txn.commit();
            return Response.ok().entity(data.targetUsername +
                    " account status changed to: " + data.newState).build();
        }catch (Exception e){
            txn.rollback();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }finally {
            if (txn.isActive())
                txn.rollback();
        }
    }
}
