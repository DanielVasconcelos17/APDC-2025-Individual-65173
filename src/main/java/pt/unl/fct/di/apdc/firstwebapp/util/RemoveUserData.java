package pt.unl.fct.di.apdc.firstwebapp.util;

public class RemoveUserData {
    public String tokenId;
    public String targetUsername;

    public RemoveUserData() {}

    public RemoveUserData(String tokenId, String targetUsername) {
        this.tokenId = tokenId;
        this.targetUsername = targetUsername;
    }
}
