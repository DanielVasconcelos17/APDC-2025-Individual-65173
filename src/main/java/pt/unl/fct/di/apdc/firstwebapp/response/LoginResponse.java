package pt.unl.fct.di.apdc.firstwebapp.response;

import pt.unl.fct.di.apdc.firstwebapp.util.AuthToken;

public class LoginResponse {
    private static final String WELCOME_RESPONSE = "Welcome, %s! Your role is: %s";

    public AuthToken token;
    public String welcomeMessage;
    public String role;

    public LoginResponse(AuthToken token, String username, String role) {
        this.token = token;
        this.welcomeMessage = String.format(WELCOME_RESPONSE, username, role);
        this.role = role;
    }
}