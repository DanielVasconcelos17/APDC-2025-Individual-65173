package pt.unl.fct.di.apdc.firstwebapp.util;

import pt.unl.fct.di.apdc.firstwebapp.types.ProfileState;

public class ChangeAccStateData {

    public String requesterUsername;
    public String targetUsername;
    public String newState;

    public ChangeAccStateData(){}

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
