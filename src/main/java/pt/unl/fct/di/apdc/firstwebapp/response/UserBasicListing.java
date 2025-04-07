package pt.unl.fct.di.apdc.firstwebapp.response;

public class UserBasicListing {

    private static final String NOT_DEFINED = "NOT DEFINED";

    public String username;
    public String email;
    public String name;

    public UserBasicListing(String username, String email, String name){
        this.username = username;
        this.email = email;
        // usa NOT DEFINED em caso de o nome ser nulo
        this.name = name != null ? name : NOT_DEFINED;
    }
}
