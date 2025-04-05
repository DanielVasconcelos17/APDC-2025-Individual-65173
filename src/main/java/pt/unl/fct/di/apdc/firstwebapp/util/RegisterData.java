package pt.unl.fct.di.apdc.firstwebapp.util;

import pt.unl.fct.di.apdc.firstwebapp.types.ProfileType;

import java.util.regex.Pattern;

public class RegisterData {

    public String username;
    public String password;
    public String pwdConfirmation;
    public String email;
    public String fullName;
    public String phone;
    public String profile; // "public" ou "private"

    // Atributos adicionais
    public String citizenCard;
    public String role;
    public String userNif;
    public String employer;
    public String job;
    public String address;
    public String employerNif;
    public String state;

    public RegisterData() {}

    public RegisterData(String username, String password, String pwdConfirmation,
                        String email, String fullName, String phone, String profile) {
        this.username = username;
        this.password = password;
        this.pwdConfirmation = pwdConfirmation;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.profile = profile;
    }

    private boolean nonEmptyOrBlankField(String field) {
        return field != null && !field.isBlank();
    }

    public boolean validRegistration() {
        return nonEmptyOrBlankField(username) &&
                nonEmptyOrBlankField(password) &&
                nonEmptyOrBlankField(pwdConfirmation) &&
                nonEmptyOrBlankField(email) &&
                nonEmptyOrBlankField(fullName) &&
                nonEmptyOrBlankField(phone) &&
                nonEmptyOrBlankField(profile) && isValidProfileType()
                && email.contains("@")
                && password.equals(pwdConfirmation);
    }



    public boolean isValidEmailAddress() {
        // Verfica se o formato do email está correto
        // com o que foi pedido no enunciado "<string>@<string>.<dom>"
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        Pattern pattern = Pattern.compile(emailRegex);
        return pattern.matcher(email).matches();
    }

    public boolean isValidProfileType(){
        return profile.equals(ProfileType.PUBLIC.getType())
                || profile.equals(ProfileType.PRIVATE.getType());
    }

    public boolean isValidPassword() {
        // Verifica se tem pelo menos uma letra minúscula, maiuscula, numero e caracter especial.
        boolean hasLower = Pattern.compile("[a-z]")
                .matcher(password)
                .find();
        boolean hasUpper = Pattern.compile("[A-Z]")
                .matcher(password)
                .find();
        boolean hasDigit = Pattern.compile("[0-9]")
                .matcher(password)
                .find();
        boolean hasSpecial = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]")
                .matcher(password)
                .find();
        return hasLower && hasUpper && hasDigit && hasSpecial;
    }
}
