package pt.unl.fct.di.apdc.firstwebapp.resources;

import com.google.cloud.datastore.*;
import jakarta.ws.rs.core.Response.Status;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.apdc.firstwebapp.authentication.TokenValidator;
import pt.unl.fct.di.apdc.firstwebapp.types.Role;
import pt.unl.fct.di.apdc.firstwebapp.util.ChangeRoleData;


@Path("/changeRole")
@Produces(MediaType.APPLICATION_JSON)
public class ChangeRoleResource {

    private static final String USER_ROLE = "user_role";

    private static final String TOKEN_ROLE = "token_role";


    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeRole(ChangeRoleData data){
        // Validação do token
        if (!TokenValidator.isValidToken(data.tokenID)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("Invalid or expired token.").build();
        }

        // Obter o role do user que está a fazer o pedido
        Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(data.tokenID);
        Entity tokenEntity = datastore.get(tokenKey);
        String requesterRole = tokenEntity.getString(TOKEN_ROLE);

        if(!ChangeRoleData.isValidRole(requesterRole)
                || !ChangeRoleData.isValidRole(data.newRole))
            return Response.status(Status.FORBIDDEN)
                    .entity("Not valid role has to be " +
                            "one of these (ENDUSER, PARTNER, ADMIN, BACKOFFICE)")
                    .build();

        // Verificar se o o user target existe
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.targetUsername);
        Entity targetEntity = datastore.get(userKey);

        if(targetEntity == null)
            return Response.status(Status.NOT_FOUND)
                    .entity("Target user not found.")
                    .build();

        // Obter a role do user target
        String targetUserRole = targetEntity.getString(USER_ROLE);

        // Verificar permissoes
        if(requesterRole.equals(Role.ENDUSER.getType()))
            return Response.status(Status.FORBIDDEN)
                    .entity("ENDUSER cannot change roles.")
                    .build();

        // Verifica se o user que faz o pedido de mudança é BACKOFFICE
        if(requesterRole.equals(Role.BACKOFFICE.getType())){
            // Verifica se o user e o role a que vai ser
            // alterado se são ENDUSER ou PARTNER
            if(!targetUserRole.equals(Role.PARTNER.getType())
                    && !targetUserRole.equals(Role.ENDUSER.getType())){
                return Response.status(Status.FORBIDDEN)
                        .entity("BACKOFFICE can only change ENDUSER to PARTNER and vice-versa.")
                        .build();
            }
            if(!data.newRole.equals(Role.PARTNER.getType())
                    && !data.newRole.equals(Role.ENDUSER.getType())){
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("BACKOFFICE can only assign ENDUSER or PARTNER roles.")
                        .build();
            }
        }
        Transaction txn = datastore.newTransaction();
        try{
            Entity updateUser = Entity.newBuilder(targetEntity)
                    .set(USER_ROLE, data.newRole).build();
            txn.put(updateUser);
            txn.commit();
            return Response.ok()
                    .entity("Role changed successfully to: " + data.newRole)
                    .build();
        }catch (Exception e){
            txn.rollback();
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }finally {
            if(txn.isActive())
                txn.rollback();
        }
    }
}
