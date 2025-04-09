package pt.unl.fct.di.apdc.firstwebapp.authentication;

import com.google.cloud.datastore.*;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.Entity;
import com.google.gson.Gson;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import pt.unl.fct.di.apdc.firstwebapp.response.TokenValidationResult;
import pt.unl.fct.di.apdc.firstwebapp.util.AuthToken;

public class TokenValidator {

    private static final String TOKEN_CREATION_DATA = "token_creationData";
    private static final String TOKEN_EXPIRATION_DATA = "token_expirationData";
    private static final String TOKEN_CHECKER = "token_checker";
    private static final String TOKEN_USERNAME = "token_username";
    private static final String TOKEN_ROLE = "token_role";

    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final Gson g = new Gson();


    public static TokenValidationResult validateToken(Transaction txn, Datastore datastore, String username) {
        try {
            // Buscar o token associado ao username
            Query<Entity> tokenQuery = Query.newEntityQueryBuilder()
                    .setKind("Token")
                    .setFilter(PropertyFilter.eq(TOKEN_USERNAME, username))
                    .build();
            QueryResults<Entity> tokens = txn.run(tokenQuery);

            if (!tokens.hasNext()) {
                return new TokenValidationResult(
                        Response.status(Status.FORBIDDEN)
                                .entity(g.toJson("No active token found for the user.")).build(),
                        null
                );
            }

            Entity tokenEntity = tokens.next();
            String tokenID = tokenEntity.getKey().getName();

            // Validação do token
            if (!TokenValidator.isValidToken(tokenID)) {
                return new TokenValidationResult(
                        Response.status(Status.FORBIDDEN)
                                .entity(g.toJson("Invalid or expired token.")).build(),
                        null
                );
            }

            return new TokenValidationResult(null, tokenEntity);
        } catch (Exception e) {
            return new TokenValidationResult(
                    Response.status(Status.INTERNAL_SERVER_ERROR).build(),
                    null
            );
        }
    }


    public static boolean isValidToken(String tokenId){
        Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(tokenId);
        Entity tokenEntity = datastore.get(tokenKey);

        if(tokenEntity == null) return false;

        String username = tokenEntity.getString(TOKEN_USERNAME);
        String role = tokenEntity.getString(TOKEN_ROLE);

        AuthToken token = new AuthToken(username, role);

        token.tokenID = tokenId;
        token.creationData = tokenEntity.getLong(TOKEN_CREATION_DATA);
        token.expirationData = tokenEntity.getLong(TOKEN_EXPIRATION_DATA);
        token.checker = tokenEntity.getString(TOKEN_CHECKER);

        return token.isValid();
    }
}
