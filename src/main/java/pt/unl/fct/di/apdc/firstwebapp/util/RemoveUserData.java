package pt.unl.fct.di.apdc.firstwebapp.util;

public class RemoveUserData {
    public String tokenID;
    public String targetUsername;

    public RemoveUserData() {}

    public RemoveUserData(String tokenID, String targetUsername) {
        this.tokenID = tokenID;
        this.targetUsername = targetUsername;
    }
}
