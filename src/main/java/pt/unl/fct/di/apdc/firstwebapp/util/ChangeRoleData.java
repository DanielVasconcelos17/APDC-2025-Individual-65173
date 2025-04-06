package pt.unl.fct.di.apdc.firstwebapp.util;

import pt.unl.fct.di.apdc.firstwebapp.types.Role;

public class ChangeRoleData {
    public String tokenID;
    public String targetUsername; // utilizador a ser alterado
    public String newRole;

    public ChangeRoleData() {}

    public ChangeRoleData(String tokenID, String targetUsername, String newRole) {
        this.tokenID = tokenID;
        this.targetUsername = targetUsername;
        this.newRole = newRole;
    }

    // Valida se o novo role é válido
    public static boolean isValidRole(String role) {
        try {
            Role.valueOf(role);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
