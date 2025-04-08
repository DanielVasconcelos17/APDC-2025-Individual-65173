package pt.unl.fct.di.apdc.firstwebapp.authentication;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import pt.unl.fct.di.apdc.firstwebapp.util.AuthToken;

public class TokenValidator {

    private static final String TOKEN_CREATION_DATA = "token_creationData";
    private static final String TOKEN_EXPIRATION_DATA = "token_expirationData";
    private static final String TOKEN_CHECKER = "token_checker";
    private static final String TOKEN_USERNAME = "token_username";
    private static final String TOKEN_ROLE = "token_role";

    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

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
