package pt.unl.fct.di.apdc.firstwebapp.util;

import pt.unl.fct.di.apdc.firstwebapp.enums.Role;

public class ChangeRoleData {
    public String requesterUsername;
    public String targetUsername; // utilizador a ser alterado
    public String newRole;

    public ChangeRoleData() {}


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
