package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
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
import pt.unl.fct.di.apdc.firstwebapp.types.ProfileState;
import pt.unl.fct.di.apdc.firstwebapp.types.Role;
import pt.unl.fct.di.apdc.firstwebapp.util.RegisterData;

@Path("/register")
@Produces(MediaType.APPLICATION_JSON)
public class RegisterResource {

	private static final String USER_PWD = "user_pwd";
	private static final String USER_EMAIL = "user_email";
	private static final String USER_ROLE = "user_role";
	private static final String USER_NAME = "user_name";
	private static final String USER_PHONE = "user_phone";
	private static final String USER_PROFILE = "user_profile";
	private static final String USER_STATE = "user_state";


	private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	private final Gson g = new Gson();


	public RegisterResource() {}


	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response registerUser(RegisterData data){
		LOG.fine("Attempt to register user: " + data.username);

		// Verificações da entrada de informação
		if(!data.validRegistration())
			return Response.status(Status.BAD_REQUEST)
					.entity(g.toJson("Missing or wrong parameter."))
					.build();
		if(!data.isValidEmailAddress()){
			return Response.status(Status.BAD_REQUEST)
					.entity(g.toJson("Invalid email format. Expected: <string>@<string>.<dom>"))
					.build();
		}
		if (!data.isValidProfileType())
			return Response.status(Status.BAD_REQUEST)
					.entity(g.toJson("Profile type invalid ('public' or 'private') !"))
					.build();
		if (!data.isValidPassword())
			return Response.status(Status.BAD_REQUEST)
					.entity(g.toJson("Password not strong enough, " +
							"must contain uppercase, lowercase, " +
							"numbers, and symbols.")).build();

		Transaction txn = datastore.newTransaction();
		try{
			Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
			Entity entity = txn.get(userKey);

			if(entity != null){
				txn.rollback();
				return Response.status(Status.CONFLICT)
						.entity(g.toJson("Username already exists !"))
						.build();
			}

			// Damos setup ao atributos obrigatórios
			Entity.Builder userBuilder = Entity.newBuilder(userKey)
					.set(USER_PWD, DigestUtils.sha512Hex(data.password))
					.set(USER_EMAIL, data.email)
					.set(USER_NAME, data.fullName)
					.set(USER_PHONE, data.phone)
					.set(USER_PROFILE, data.profile)
					.set(USER_ROLE, Role.ENDUSER.getType())
					.set(USER_STATE, ProfileState.DEACTIVATE.getType())
					.set("user_creation_time", Timestamp.now());

			// Caso forneçam os "atributos opcionais"
			if (data.citizenCard != null) userBuilder.set("user_citizen_card", data.citizenCard);
			if (data.userNif != null) userBuilder.set("user_nif", data.userNif);
			if (data.employer != null) userBuilder.set("user_employer", data.employer);
			if (data.job != null) userBuilder.set("user_job", data.job);
			if (data.address != null) userBuilder.set("user_address", data.address);
			if (data.employerNif != null) userBuilder.set("user_employer_nif", data.employerNif);

			txn.put(userBuilder.build());
			txn.commit();

			LOG.info("User registered successfully: " + data.username);
			return Response.ok(g.toJson("User registered with success !"))
					.build();
		}catch (Exception e){
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}finally {
			if(txn.isActive())
				txn.rollback();
		}
	}
}
