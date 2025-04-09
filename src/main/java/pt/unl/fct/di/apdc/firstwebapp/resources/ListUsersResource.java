package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.logging.Logger;

import com.google.cloud.datastore.*;
import com.google.gson.Gson;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.MediaType;
import pt.unl.fct.di.apdc.firstwebapp.authentication.TokenValidator;
import pt.unl.fct.di.apdc.firstwebapp.response.UserBasicListing;
import pt.unl.fct.di.apdc.firstwebapp.response.UserFullListing;
import pt.unl.fct.di.apdc.firstwebapp.enums.ProfileState;
import pt.unl.fct.di.apdc.firstwebapp.enums.ProfileType;
import pt.unl.fct.di.apdc.firstwebapp.enums.Role;
import pt.unl.fct.di.apdc.firstwebapp.enums.UserDSFields;
import pt.unl.fct.di.apdc.firstwebapp.util.AuthToken;

import java.util.ArrayList;
import java.util.List;

@Path("/listUsers")
@Produces(MediaType.APPLICATION_JSON)
public class ListUsersResource {

    private static final String TOKEN_ROLE = "token_role";
    private static final String TOKEN_USERNAME = "token_username";


    private static final Logger LOG = Logger.getLogger(ListUsersResource.class.getName());

    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final Gson g = new Gson();

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response listUsers(AuthToken token) {

        // Buscar o tokenID associado ao requesterUsername
        Query<Entity> tokenQuery = Query.newEntityQueryBuilder()
                .setKind("Token")
                .setFilter(StructuredQuery.PropertyFilter.eq(TOKEN_USERNAME, token.username))
                .build();
        QueryResults<Entity> allTokens = datastore.run(tokenQuery);

        if (!allTokens.hasNext()) {
            return Response.status(Status.FORBIDDEN)
                    .entity(g.toJson("No active token found for requester."))
                    .build();
        }

        Entity tokenEntity = allTokens.next();
        String tokenID = tokenEntity.getKey().getName(); // Obtém o ID do token
        String tokenRole = tokenEntity.getString(TOKEN_ROLE); // Obtém o ID do token


        if (!TokenValidator.isValidToken(tokenID)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(g.toJson("Invalid or expired token."))
                    .build();
        }

        try {
            List<UserBasicListing> result = new ArrayList<>();
            //Retorna o set de resultados do tipo "User"
            QueryResults<Entity> users =
                    datastore.run(Query.newEntityQueryBuilder()
                            .setKind("User").build());
            while (users.hasNext()) {
                Entity user = users.next();
                String userRole = user.getString(UserDSFields.USER_ROLE.toString());
                String userProfile = user.getString(UserDSFields.USER_PROFILE.toString());
                String userState = user.getString(UserDSFields.USER_STATE.toString());

                if (shouldIncludeUser(tokenRole, userRole, userProfile, userState)) {
                    result.add(createUserDS(tokenRole, user));
                }
            }
            return Response.ok(g.toJson(result)).build();
        } catch (Exception e) {
            LOG.severe("Error listing users: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

    }

    // Verifica os requisitos impostos por cada role associado ao token
    private boolean shouldIncludeUser(String tokenRole, String userRole,
                                      String userProfile, String userState) {
        Role tknRole = Role.valueOf(tokenRole);
        switch (tknRole) {
            case ADMIN -> {
                return true;
            }
            case BACKOFFICE -> {
                return userRole.equals(Role.ENDUSER.getType());
            }
            case ENDUSER -> {
                return userRole.equals(Role.ENDUSER.getType())
                        && userProfile.equals(ProfileType.PUBLIC.getType())
                        && userState.equals(ProfileState.ACTIVATE.getType());
            }
            default -> {
                return false;
            }
        }

    }

    // Cria o suporte de listagem com base nos campos do datastore
    private UserBasicListing createUserDS(String requesterRole, Entity user) {
        Role requester = Role.valueOf(requesterRole);
        return switch (requester) {
            case Role.ADMIN, Role.BACKOFFICE -> new UserFullListing(user);
            default -> new UserBasicListing(
                    user.getKey().getName(),
                    user.getString(UserDSFields.USER_EMAIL.toString()),
                    user.getString(UserDSFields.USER_NAME.toString())
            );
        };
    }
}
