package pt.unl.fct.di.apdc.firstwebapp.util;

import pt.unl.fct.di.apdc.firstwebapp.types.ProfileState;

public class ChangeAccStateData {

    public String tokenID;
    public String targetUsername;
    public String newState;

    public ChangeAccStateData(){}

    public ChangeAccStateData(String tokenID, String targetUsername, String newState){
        this.tokenID = tokenID;
        this.targetUsername = targetUsername;
        this.newState = newState;
    }

    //Verifica se o state é válido
    public static boolean isValidState(String state){
        try {
            ProfileState.valueOf(state);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

}
