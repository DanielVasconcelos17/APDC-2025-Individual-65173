package pt.unl.fct.di.apdc.firstwebapp.resources;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.apdc.firstwebapp.util.ChangeRoleData;


@Path("/changeRole")
@Produces(MediaType.APPLICATION_JSON)
public class ChangeRoleResource {


    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeRole(ChangeRoleData data){

        return Response.ok().build();
    }
}
