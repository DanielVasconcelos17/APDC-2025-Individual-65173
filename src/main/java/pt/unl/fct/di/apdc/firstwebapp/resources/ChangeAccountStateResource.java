package pt.unl.fct.di.apdc.firstwebapp.resources;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.apdc.firstwebapp.authentication.TokenValidator;
import pt.unl.fct.di.apdc.firstwebapp.util.ChangeAccStateData;

@Path("/changeState")
@Produces(MediaType.APPLICATION_JSON)
public class ChangeAccountStateResource {

    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeAccState(ChangeAccStateData data){
        if (!TokenValidator.isValidToken(data.tokenID)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("Invalid or expired token.").build();
        }

        return Response.ok().build();
    }
}
