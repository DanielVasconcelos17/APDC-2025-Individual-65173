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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import pt.unl.fct.di.apdc.firstwebapp.types.ProfileState;
import pt.unl.fct.di.apdc.firstwebapp.types.Role;
import pt.unl.fct.di.apdc.firstwebapp.util.RegisterData;

@Path("/register")
public class RegisterResource {

	private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	private final Gson g = new Gson();


	public RegisterResource() {}


	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response registerUser(RegisterData data){
		LOG.fine("Attempt to register user: " + data.username);

		// Verificações da entrada de informação
		if(!data.validRegistration())
			return Response.status(Status.BAD_REQUEST)
					.entity("Missing or wrong parameter.")
					.build();
		if(!data.isValidEmailAddress()){
			return Response.status(Status.BAD_REQUEST)
					.entity("Invalid email format. Expected: <string>@<string>.<dom>")
					.build();
		}
		if (!data.isValidProfileType())
			return Response.status(Status.BAD_REQUEST)
					.entity("Profile type invalid ('público' or 'privado') !")
					.build();
		if (!data.isValidPassword())
			return Response.status(Status.BAD_REQUEST)
					.entity("Password not strong enough, " +
							"must contain uppercase, lowercase, " +
							"numbers, and symbols.").build();

		Transaction txn = datastore.newTransaction();
		try{
			Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
			Entity entity = txn.get(userKey);

			if(entity != null){
				txn.rollback();
				return Response.status(Status.CONFLICT)
						.entity("Username already exists !")
						.build();
			}

			// Damos setup ao atributos obrigatórios
			Entity.Builder userBuilder = Entity.newBuilder(userKey)
					.set("user_pwd", DigestUtils.sha512Hex(data.password))
					.set("user_email", data.email)
					.set("user_name", data.fullName)
					.set("user_phone", data.phone)
					.set("user_profile", data.profile)
					.set("user_role", Role.ENDUSER.getType())
					.set("user_state", ProfileState.DESATIVADA.getType())
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
			return Response.ok().entity("User registered with success !").build();
		}catch (DatastoreException e){
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(e.toString())
					.build();
		}finally {
			if(txn.isActive())
				txn.rollback();
		}
	}
}
