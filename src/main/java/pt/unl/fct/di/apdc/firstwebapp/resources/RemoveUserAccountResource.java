package pt.unl.fct.di.apdc.firstwebapp.resources;


import com.google.cloud.datastore.*;
import com.google.gson.Gson;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.apdc.firstwebapp.authentication.TokenValidator;
import pt.unl.fct.di.apdc.firstwebapp.response.TokenValidationResult;
import pt.unl.fct.di.apdc.firstwebapp.enums.Role;
import pt.unl.fct.di.apdc.firstwebapp.util.RemoveUserData;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;

@Path("/removeUser")
@Produces(MediaType.APPLICATION_JSON)
public class RemoveUserAccountResource {

    private static final String USER_ROLE = "user_role";

    private static final String TOKEN_ROLE = "token_role";
    private static final String TOKEN_USERNAME = "token_username";


    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

    private final Gson g = new Gson();


    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeUserAcc(RemoveUserData data) {

        Transaction txn = datastore.newTransaction();
        try {
            // Buscar o tokenID associado ao requesterUsername
            TokenValidationResult validation =
                    TokenValidator.validateToken(txn, datastore, data.requesterUsername);
            if (validation.errorResponse != null) {
                txn.rollback();
                return validation.errorResponse;
            }

            // Obter role do user que faz o pedido
            String requesterRole = validation.tokenEntity.getString(TOKEN_ROLE);

            // Verificar se o target é o próprio root
            if (data.targetUsername.equals("root"))
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(g.toJson("Cannot remove root account.")).build();

            Key userKey = userKeyFactory.newKey(data.targetUsername);
            Entity targetUser = txn.get(userKey);

            if (targetUser == null)
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(g.toJson("Target user not found.")).build();

            String targetRole = targetUser.getString(USER_ROLE);

            if (!requesterRole.equals(Role.ADMIN.getType())) {
                if (requesterRole.equals(Role.BACKOFFICE.getType())) {
                    // BACKOFFICE só pode remover ENDUSER/PARTNER
                    if (!targetRole.equals(Role.ENDUSER.getType())
                            && !targetRole.equals(Role.PARTNER.getType()))
                        return Response.status(Response.Status.FORBIDDEN)
                                .entity(g.toJson("BACKOFFICE can only remove " +
                                        "ENDUSER/PARTNER accounts.")).build();
                }
                // Qualquer outra role não tem permissão
                else
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity(g.toJson("You don't have permissions " +
                                    "to remove users.")).build();
            }

            // Remover tokens associados através do username
            Query<Entity> query = Query.newEntityQueryBuilder()
                    .setKind("Token")
                    .setFilter(PropertyFilter.eq(TOKEN_USERNAME, data.targetUsername))
                    .build();
            QueryResults<Entity> tokens = txn.run(query);
            tokens.forEachRemaining(token -> txn.delete(token.getKey()));

            // Remover o utilizador
            txn.delete(userKey);
            txn.commit();

            return Response.ok(g.toJson("User removed successfully.")).build();
        } catch (Exception e) {
            txn.rollback();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(g.toJson("Error removing user.")).build();
        } finally {
            if (txn.isActive())
                txn.rollback();
        }
    }
}
