package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import pt.unl.fct.di.apdc.firstwebapp.authentication.EmailValidator;
import pt.unl.fct.di.apdc.firstwebapp.types.ProfileState;
import pt.unl.fct.di.apdc.firstwebapp.types.Role;
import pt.unl.fct.di.apdc.firstwebapp.types.UserDSFields;
import pt.unl.fct.di.apdc.firstwebapp.util.RegisterData;

@Path("/register")
@Produces(MediaType.APPLICATION_JSON)
public class RegisterResource {

    private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    private final Gson g = new Gson();


    public RegisterResource() {
    }


    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerUser(RegisterData data) {
        LOG.fine("Attempt to register user: " + data.username);

        Transaction txn = datastore.newTransaction();
        try {
            // Verificações da entrada de informação
            if (!data.validRegistration())
                return Response.status(Status.BAD_REQUEST)
                        .entity(g.toJson("Missing or wrong parameter."))
                        .build();
            if (!EmailValidator.isValidEmailAddress(data.email))
                return Response.status(Status.BAD_REQUEST)
                        .entity(g.toJson("Invalid email format. Expected: <string>@<string>.<dom>"))
                        .build();

            if (!data.isValidProfileType())
                return Response.status(Status.BAD_REQUEST)
                        .entity(g.toJson("Profile type invalid ('public' or 'private') !"))
                        .build();
            if (!data.isValidPassword())
                return Response.status(Status.BAD_REQUEST)
                        .entity(g.toJson("Password not strong enough, " +
                                "must contain uppercase, lowercase, " +
                                "numbers, and symbols.")).build();


            Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
            Entity entity = txn.get(userKey);

            if (entity != null) {
                txn.rollback();
                return Response.status(Status.CONFLICT)
                        .entity(g.toJson("Username already exists !"))
                        .build();
            }

            // Damos setup ao atributos obrigatórios
            Entity.Builder userBuilder = Entity.newBuilder(userKey)
                    .set(UserDSFields.USER_PWD.toString(),
                            DigestUtils.sha512Hex(data.password))
                    .set(UserDSFields.USER_EMAIL.toString()
                            , data.email)
                    .set(UserDSFields.USER_NAME.toString()
                            , data.fullName)
                    .set(UserDSFields.USER_PHONE.toString()
                            , data.phone)
                    .set(UserDSFields.USER_PROFILE.toString()
                            , data.profile)
                    .set(UserDSFields.USER_ROLE.toString()
                            , Role.ENDUSER.getType())
                    .set(UserDSFields.USER_STATE.toString()
                            , ProfileState.DEACTIVATE.getType())
                    .set("user_creation_time", Timestamp.now());

            // Caso forneçam os "atributos opcionais"
            if (data.citizenCard != null)
                userBuilder.set(UserDSFields.USER_CC.toString(), data.citizenCard);
            if (data.userNif != null)
                userBuilder.set(UserDSFields.USER_NIF.toString(), data.userNif);
            if (data.employer != null)
                userBuilder.set(UserDSFields.USER_EMPLOYER.toString(), data.employer);
            if (data.job != null)
                userBuilder.set(UserDSFields.USER_JOB.toString(), data.job);
            if (data.address != null)
                userBuilder.set(UserDSFields.USER_ADDRESS.toString(), data.address);
            if (data.employerNif != null)
                userBuilder.set(UserDSFields.USER_EMP_NIF.toString(), data.employerNif);

            txn.put(userBuilder.build());
            txn.commit();

            LOG.info("User registered successfully: " + data.username);
            return Response.ok(g.toJson("User registered with success !"))
                    .build();
        } catch (Exception e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (txn.isActive())
                txn.rollback();
        }
    }
}
