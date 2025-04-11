package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
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
import pt.unl.fct.di.apdc.firstwebapp.enums.UserDSFields;
import pt.unl.fct.di.apdc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.apdc.firstwebapp.util.LoginData;
import pt.unl.fct.di.apdc.firstwebapp.response.LoginResponse;

@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {

    private static final String MESSAGE_INVALID_PASSWORD = "Incorrect password.";
    private static final String MESSAGE_INVALID_IDENTIFIER = "Non-existent identifier.";

    private static final String MESSAGE_INVALID_CREDENTIALS = "Incorrect username or password.";
    private static final String MESSAGE_NEXT_PARAMETER_INVALID = "Request parameter 'next' must be greater or equal to 0.";

    private static final String LOG_MESSAGE_LOGIN_ATTEMP = "Login attempt by user: ";
    private static final String LOG_MESSAGE_LOGIN_SUCCESSFUL = "Login successful by user: ";
    private static final String LOG_MESSAGE_WRONG_PASSWORD = "Wrong password for: ";
    private static final String LOG_MESSAGE_UNKNOW_USER = "Failed login attempt for username: ";


    private static final String TOKEN_CREATION_DATA = "token_creationData";
    private static final String TOKEN_EXPIRATION_DATA = "token_expirationData";
    private static final String TOKEN_CHECKER = "token_checker";
    private static final String TOKEN_USERNAME = "token_username";
    private static final String TOKEN_ROLE = "token_role";


    private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

    private final Gson g = new Gson();

    public LoginResource() {}

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response doLogin(LoginData data) {
        LOG.fine(LOG_MESSAGE_LOGIN_ATTEMP + data.identifier);

        Transaction txn = datastore.newTransaction();
        try {
            Query<Entity> query;
            if (EmailValidator.isValidEmailAddress(data.identifier))
                // Busca por email
                query = Query.newEntityQueryBuilder()
                        .setKind("User")
                        .setFilter(PropertyFilter.eq(UserDSFields.USER_EMAIL.toString(), data.identifier))
                        .build();
            else
                // Busca por username
                query = Query.newEntityQueryBuilder()
                        .setKind("User")
                        .setFilter(PropertyFilter.eq("__key__",
                                userKeyFactory.newKey(data.identifier)))
                        .build();

            QueryResults<Entity> results = txn.run(query);

            if (!results.hasNext()) {
                txn.rollback();
                LOG.warning(LOG_MESSAGE_UNKNOW_USER + data.identifier);
                return Response.status(Status.FORBIDDEN)
                        .entity(g.toJson(MESSAGE_INVALID_IDENTIFIER)).build();
            }

            Entity user = results.next();
            String hashedPWD = user.getString(UserDSFields.USER_PWD.toString());
            if (!hashedPWD.equals(DigestUtils.sha512Hex(data.password))) {
                txn.rollback();
                LOG.warning(LOG_MESSAGE_WRONG_PASSWORD + data.identifier);
                return Response.status(Status.FORBIDDEN)
                        .entity(g.toJson(MESSAGE_INVALID_PASSWORD)).build();
            }

            // Verificar e remover token existente, se houver
            Query<Entity> tokenQuery = Query.newEntityQueryBuilder()
                    .setKind("Token")
                    .setFilter(PropertyFilter.eq(TOKEN_USERNAME, data.identifier))
                    .build();
            QueryResults<Entity> existingTokens = txn.run(tokenQuery);

            while (existingTokens.hasNext()) {
                Entity existingToken = existingTokens.next();
                txn.delete(existingToken.getKey());
            }

            String userRole = user.getString(UserDSFields.USER_ROLE.toString());
            AuthToken token = new AuthToken(data.identifier, userRole);

            // Guardar o token
            Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(token.tokenID);
            Entity tokenEntity = Entity.newBuilder(tokenKey)
                    .set(TOKEN_USERNAME, data.identifier)
                    .set(TOKEN_ROLE, userRole)
                    .set(TOKEN_CREATION_DATA, token.creationData)
                    .set(TOKEN_EXPIRATION_DATA, token.expirationData)
                    .set(TOKEN_CHECKER, token.checker)
                    .build();
            txn.put(tokenEntity);
            txn.commit();
            LoginResponse response = new LoginResponse(token, data.identifier, userRole);
            LOG.info(LOG_MESSAGE_LOGIN_SUCCESSFUL + data.identifier);
            return Response.ok(g.toJson(response)).build();

        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Error during login: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (txn.isActive())
                txn.rollback();
        }
    }
}
