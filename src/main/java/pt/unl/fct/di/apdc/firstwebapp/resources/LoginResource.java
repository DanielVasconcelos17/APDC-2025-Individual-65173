package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.*;
import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.OrderBy;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response.Status;
import pt.unl.fct.di.apdc.firstwebapp.authentication.EmailValidator;
import pt.unl.fct.di.apdc.firstwebapp.types.UserDSFields;
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

    private static final String USER_PWD = "user_pwd";
    private static final String USER_LOGIN_TIME = "user_login_time";
    private static final String USER_LOGIN_ROLE = "user_role";

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

            QueryResults<Entity> results = datastore.run(query);

            if (!results.hasNext()) {
                txn.rollback();
                LOG.warning(LOG_MESSAGE_UNKNOW_USER + data.identifier);
                return Response.status(Status.FORBIDDEN)
                        .entity(g.toJson(MESSAGE_INVALID_IDENTIFIER)).build();
            }

            Entity user = results.next();
            String hashedPWD = user.getString(USER_PWD);
            if (!hashedPWD.equals(DigestUtils.sha512Hex(data.password))) {
                txn.rollback();
                LOG.warning(LOG_MESSAGE_WRONG_PASSWORD + data.identifier);
                return Response.status(Status.FORBIDDEN)
                        .entity(g.toJson(MESSAGE_INVALID_PASSWORD)).build();
            }

            String userRole = user.getString(USER_LOGIN_ROLE);
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


    @POST
    @Path("/v1a")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response doLoginV1a(LoginData data) {
        LOG.fine(LOG_MESSAGE_LOGIN_ATTEMP + data.identifier);

        Key userKey = userKeyFactory.newKey(data.identifier);

        Entity user = datastore.get(userKey);
        if (user != null) {
            String hashedPWD = (String) user.getString(USER_PWD);
            if (hashedPWD.equals(DigestUtils.sha512Hex(data.password))) {
                user = Entity.newBuilder(user)
                        .set("user_login_time", Timestamp.now())
                        .build();
                datastore.update(user);
                LOG.info(LOG_MESSAGE_LOGIN_SUCCESSFUL + data.identifier);
                AuthToken token = new AuthToken(data.identifier, "role");
                return Response.ok(g.toJson(token)).build();
            } else {
                LOG.warning(LOG_MESSAGE_WRONG_PASSWORD + data.identifier);
                return Response.status(Status.FORBIDDEN)
                        .entity(MESSAGE_INVALID_CREDENTIALS)
                        .build();
            }
        } else {
            LOG.warning(LOG_MESSAGE_UNKNOW_USER + data.identifier);
            return Response.status(Status.FORBIDDEN)
                    .entity(MESSAGE_INVALID_CREDENTIALS)
                    .build();
        }
    }

    @POST
    @Path("/v1b")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response doLoginV1b(LoginData data) {
        LOG.fine(LOG_MESSAGE_LOGIN_ATTEMP + data.identifier);

        Key userKey = userKeyFactory.newKey(data.identifier);

        Entity user = datastore.get(userKey);
        if (user != null) {
            String hashedPWD = user.getString(USER_PWD);
            if (hashedPWD.equals(DigestUtils.sha512Hex(data.password))) {
                KeyFactory logKeyFactory = datastore.newKeyFactory()
                        .addAncestor(PathElement.of("User", data.identifier))
                        .setKind("UserLog");
                Key logKey = datastore.allocateId(logKeyFactory.newKey());
                Entity userLog = Entity.newBuilder(logKey)
                        .set("user_login_time", Timestamp.now())
                        .build();
                datastore.put(userLog);
                LOG.info(LOG_MESSAGE_LOGIN_SUCCESSFUL + data.identifier);
                AuthToken token = new AuthToken(data.identifier, "role");
                return Response.ok(g.toJson(token)).build();
            } else {
                LOG.warning(LOG_MESSAGE_WRONG_PASSWORD + data.identifier);
                return Response.status(Status.FORBIDDEN)
                        .entity(MESSAGE_INVALID_CREDENTIALS)
                        .build();
            }
        } else {
            LOG.warning(LOG_MESSAGE_UNKNOW_USER + data.identifier);
            return Response.status(Status.FORBIDDEN)
                    .entity(MESSAGE_INVALID_CREDENTIALS)
                    .build();
        }
    }

    @POST
    @Path("/v2")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response doLoginV2(LoginData data,
                              @Context HttpServletRequest request,
                              @Context HttpHeaders headers) {
        LOG.fine(LOG_MESSAGE_LOGIN_ATTEMP + data.identifier);

        Key userKey = userKeyFactory.newKey(data.identifier);
        Key ctrsKey = datastore.newKeyFactory()
                .addAncestors(PathElement.of("User", data.identifier))
                .setKind("UserStats")
                .newKey("counters");
        // Generate automatically a key
        Key logKey = datastore.allocateId(
                datastore.newKeyFactory()
                        .addAncestors(PathElement.of("User", data.identifier))
                        .setKind("UserLog").newKey());

        Transaction txn = datastore.newTransaction();
        try {
            Entity user = txn.get(userKey);
            if (user == null) {
                // Username does not exist
                LOG.warning(LOG_MESSAGE_LOGIN_ATTEMP + data.identifier);
                return Response.status(Status.FORBIDDEN)
                        .entity(MESSAGE_INVALID_CREDENTIALS)
                        .build();
            }

            // We get the user stats from the storage
            Entity stats = txn.get(ctrsKey);
            if (stats == null) {
                stats = Entity.newBuilder(ctrsKey)
                        .set("user_stats_logins", 0L)
                        .set("user_stats_failed", 0L)
                        .set("user_first_login", Timestamp.now())
                        .set("user_last_login", Timestamp.now())
                        .build();
            }

            String hashedPWD = (String) user.getString(USER_PWD);
            if (hashedPWD.equals(DigestUtils.sha512Hex(data.password))) {
                // Login successful
                // Construct the logs
                String cityLatLong = headers.getHeaderString("X-AppEngine-CityLatLong");
                Entity log = Entity.newBuilder(logKey)
                        .set("user_login_ip", request.getRemoteAddr())
                        .set("user_login_host", request.getRemoteHost())
                        .set("user_login_latlon", cityLatLong != null
                                ? StringValue.newBuilder(cityLatLong).setExcludeFromIndexes(true).build()
                                : StringValue.newBuilder("").setExcludeFromIndexes(true).build())
                        .set("user_login_city", headers.getHeaderString("X-AppEngine-City"))
                        .set("user_login_country", headers.getHeaderString("X-AppEngine-Country"))
                        .set("user_login_time", Timestamp.now())
                        .build();

                // Get the user statistics and updates it
                // Copying information every time a user logins may not be a good solution
                // (why?)
                Entity ustats = Entity.newBuilder(ctrsKey)
                        .set("user_stats_logins", stats.getLong("user_stats_logins") + 1)
                        .set("user_stats_failed", 0L)
                        .set("user_first_login", stats.getTimestamp("user_first_login"))
                        .set("user_last_login", Timestamp.now())
                        .build();

                // Batch operation
                txn.put(log, ustats);
                txn.commit();
                // Return token
                AuthToken token = new AuthToken(data.identifier, "role");
                LOG.info(LOG_MESSAGE_LOGIN_SUCCESSFUL + data.identifier);
                return Response.ok(g.toJson(token)).build();
            } else {
                // Incorrect password
                // Copying here is even worse. Propose a better solution!
                Entity ustats = Entity.newBuilder(ctrsKey)
                        .set("user_stats_logins", stats.getLong("user_stats_logins"))
                        .set("user_stats_failed", stats.getLong("user_stats_failed") + 1L)
                        .set("user_first_login", stats.getTimestamp("user_first_login"))
                        .set("user_last_login", stats.getTimestamp("user_last_login"))
                        .set("user_last_attempt", Timestamp.now())
                        .build();

                txn.put(ustats);
                txn.commit();
                LOG.warning(LOG_MESSAGE_WRONG_PASSWORD + data.identifier);
                return Response.status(Status.FORBIDDEN).entity(MESSAGE_INVALID_CREDENTIALS).build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe(e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    @POST
    @Path("/user")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLatestLogins(LoginData data) {

        Key userKey = userKeyFactory.newKey(data.identifier);

        Entity user = datastore.get(userKey);
        if (user != null && user.getString(USER_PWD).equals(DigestUtils.sha512Hex(data.password))) {

            // Get the date of yesterday
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -1);
            Timestamp yesterday = Timestamp.of(cal.getTime());

            Query<Entity> query = Query.newEntityQueryBuilder()
                    .setKind("UserLog")
                    .setFilter(
                            CompositeFilter.and(
                                    PropertyFilter.hasAncestor(
                                            datastore.newKeyFactory().setKind("User").newKey(data.identifier)),
                                    PropertyFilter.ge(USER_LOGIN_TIME, yesterday)))
                    .setOrderBy(OrderBy.desc(USER_LOGIN_TIME))
                    .setLimit(3)
                    .build();
            QueryResults<Entity> logs = datastore.run(query);

            List<Date> loginDates = new ArrayList<Date>();
            logs.forEachRemaining(userlog -> {
                loginDates.add(userlog.getTimestamp(USER_LOGIN_TIME).toDate());
            });

            return Response.ok(g.toJson(loginDates)).build();
        }
        return Response.status(Status.FORBIDDEN).entity(MESSAGE_INVALID_CREDENTIALS)
                .build();
    }

    @POST
    @Path("/user/pagination")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLatestLogins(@QueryParam("next") String nextParam, LoginData data) {

        int next;

        // Checking for valid request parameter values
        try {
            next = Integer.parseInt(nextParam);
            if (next < 0)
                return Response.status(Status.BAD_REQUEST).entity(MESSAGE_NEXT_PARAMETER_INVALID).build();
        } catch (NumberFormatException e) {
            return Response.status(Status.BAD_REQUEST).entity(MESSAGE_NEXT_PARAMETER_INVALID).build();
        }

        Key userKey = userKeyFactory.newKey(data.identifier);

        Entity user = datastore.get(userKey);
        if (user != null && user.getString(USER_PWD).equals(DigestUtils.sha512Hex(data.password))) {

            // Get the date of yesterday
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -1);
            Timestamp yesterday = Timestamp.of(cal.getTime());

            Query<Entity> query = Query.newEntityQueryBuilder()
                    .setKind("UserLog")
                    .setFilter(
                            CompositeFilter.and(
                                    PropertyFilter.hasAncestor(
                                            datastore.newKeyFactory().setKind("User").newKey(data.identifier)),
                                    PropertyFilter.ge(USER_LOGIN_TIME, yesterday)))
                    .setOrderBy(OrderBy.desc(USER_LOGIN_TIME))
                    .setLimit(3)
                    .setOffset(next)
                    .build();
            QueryResults<Entity> logs = datastore.run(query);

            List<Date> loginDates = new ArrayList<Date>();
            logs.forEachRemaining(userlog -> {
                loginDates.add(userlog.getTimestamp(USER_LOGIN_TIME).toDate());
            });

            return Response.ok(g.toJson(loginDates)).build();
        }
        return Response.status(Status.FORBIDDEN).entity(MESSAGE_INVALID_CREDENTIALS)
                .build();
    }

}
