package pt.unl.fct.di.apdc.firstwebapp.resources;

import com.google.cloud.datastore.*;
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
import pt.unl.fct.di.apdc.firstwebapp.response.TokenValidationResult;
import pt.unl.fct.di.apdc.firstwebapp.util.LogoutData;

import java.util.logging.Logger;

@Path("/logout")
@Produces(MediaType.APPLICATION_JSON)
public class LogoutResource {

    private static final String TOKEN_USERNAME = "token_username";

    private static final Logger LOG = Logger.getLogger(LogoutResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    private final Gson g = new Gson();

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response doLogout(LogoutData data) {

        Transaction txn = datastore.newTransaction();
        try {
            // Buscar o token associado ao username
            TokenValidationResult validation =
                    TokenValidator.validateToken(txn, datastore, data.username);
            if (validation.errorResponse != null) {
                txn.rollback();
                return validation.errorResponse;
            }

            Query<Entity> query = Query.newEntityQueryBuilder()
                    .setKind("Token")
                    .setFilter(PropertyFilter.eq(TOKEN_USERNAME, data.username))
                    .build();

            QueryResults<Entity> results = txn.run(query);

            //Remove todos os tokens associados ao username em questao de forma a garantir o logout
            while(results.hasNext()){
                Entity token = results.next();
                txn.delete(token.getKey());
            }
            txn.commit();

            return Response.ok(g.toJson("Logout successful. Token revoked.")).build();

        } catch (Exception e) {
            txn.rollback();
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (txn.isActive())
                txn.rollback();
        }
    }
}
