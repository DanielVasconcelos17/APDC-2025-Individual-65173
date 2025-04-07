package pt.unl.fct.di.apdc.firstwebapp.types;

public enum UserDSFields {
    // Campos básicos (herdados de UserBasicListing)
    USER_EMAIL("user_email"),
    USER_NAME("user_name"),

    USER_PHONE("user_phone"),
    USER_ROLE("user_role"),
    USER_STATE("user_state"),
    USER_PWD("user_pwd"),
    USER_PROFILE("user_profile"),

    USER_CC("user_citizen_card"),
    USER_NIF("user_nif"),
    USER_EMPLOYER("user_employer"),
    USER_JOB("user_job"),
    USER_EMP_NIF("user_employer_nif"),
    USER_ADDRESS("user_address");

    private final String fieldName;

    UserDSFields(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public String toString() {
        return fieldName;
    }
}
