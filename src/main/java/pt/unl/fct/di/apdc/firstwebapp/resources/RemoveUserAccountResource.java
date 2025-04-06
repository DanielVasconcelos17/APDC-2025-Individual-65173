package pt.unl.fct.di.apdc.firstwebapp.resources;


import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.gson.Gson;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.logging.Logger;

@Path("/removeAcc")
@Produces(MediaType.APPLICATION_JSON)
public class RemoveUserAccountResource {

    private static final Logger LOG = Logger.getLogger(RemoveUserAccountResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();


    private final Gson g = new Gson();


    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeUserAcc(){

        return Response.ok().build();
    }
}
