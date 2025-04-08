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
import org.apache.commons.codec.digest.DigestUtils;
import pt.unl.fct.di.apdc.firstwebapp.authentication.TokenValidator;
import pt.unl.fct.di.apdc.firstwebapp.response.UserFullListing;
import pt.unl.fct.di.apdc.firstwebapp.types.UserDSFields;
import pt.unl.fct.di.apdc.firstwebapp.util.ChangePasswordData;

@Path("/changePassword")
@Produces(MediaType.APPLICATION_JSON)
public class ChangePasswordResource {

    private static final String MESSAGE_PASSWORD_MISMATCH = "New passwords do not match.";
    private static final String MESSAGE_INVALID_CURRENT_PASSWORD = "Current password is incorrect.";


    private static final String TOKEN_USERNAME = "token_username";

    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final Gson g = new Gson();

    private static final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind("Token");
    private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");


    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changePassword(ChangePasswordData data){
        // Validação do token
        if (!TokenValidator.isValidToken(data.tokenID)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(g.toJson("Invalid or expired token.")).build();
        }
        Transaction txn = datastore.newTransaction();
        try{
            Key tokenKey = tokenKeyFactory.newKey(data.tokenID);
            Entity tokenEntity = txn.get(tokenKey);
            String username = tokenEntity.getString(TOKEN_USERNAME);

            Key userKey = userKeyFactory.newKey(username);
            Entity userEntity = txn.get(userKey);

            String currPasswordHashed = userEntity.getString(UserDSFields.USER_PWD.toString());
            if(!currPasswordHashed.equals(DigestUtils.sha512Hex(data.currentPassword))){
                txn.rollback();
                return Response.status(Status.FORBIDDEN)
                        .entity(g.toJson(MESSAGE_INVALID_CURRENT_PASSWORD))
                        .build();
            }

            if(!data.newPassword.equals(data.confirmNewPassword)){
                txn.rollback();
                return Response.status(Status.BAD_REQUEST)
                        .entity(g.toJson(MESSAGE_PASSWORD_MISMATCH))
                        .build();
            }
            Entity updatedUser = Entity.newBuilder(userEntity)
                    .set(UserDSFields.USER_PWD.toString(),
                            DigestUtils.sha512Hex(data.newPassword))
                    .build();
            txn.put(updatedUser);
            txn.commit();
            //Para visualização dos atributos
            UserFullListing view = new UserFullListing(updatedUser);
            return Response.ok(g.toJson(view)).build();
        }catch (Exception e){
            txn.rollback();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }finally {
            if(txn.isActive())
                txn.rollback();
        }
    }
}
