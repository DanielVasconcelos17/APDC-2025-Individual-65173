package pt.unl.fct.di.apdc.firstwebapp.response;

import com.google.cloud.datastore.*;
import pt.unl.fct.di.apdc.firstwebapp.types.UserDSFields;


public class UserFullListing extends UserBasicListing{

    private static final String USER_EMAIL = UserDSFields.USER_EMAIL.toString();
    private static final String USER_NAME = UserDSFields.USER_NAME.toString();
    private static final String USER_PHONE = UserDSFields.USER_PHONE.toString();
    private static final String USER_ROLE = UserDSFields.USER_ROLE.toString();
    private static final String USER_STATE = UserDSFields.USER_STATE.toString();
    private static final String USER_PWD = UserDSFields.USER_PWD.toString();
    private static final String USER_PROFILE = UserDSFields.USER_PROFILE.toString();

    private static final String USER_CC = UserDSFields.USER_CC.toString();
    private static final String USER_NIF = UserDSFields.USER_NIF.toString();
    private static final String USER_EMPLOYER = UserDSFields.USER_EMPLOYER.toString();
    private static final String USER_JOB = UserDSFields.USER_JOB.toString();
    private static final String USER_ADDRESS = UserDSFields.USER_ADDRESS.toString();
    private static final String USER_EMP_NIF = UserDSFields.USER_EMP_NIF.toString();


    private static final String NOT_DEFINED = "NOT DEFINED";

    public String phone;
    public String role;
    public String state;
    public String password;
    public String profile;

    public String citizenCard;
    public String userNif;
    public String employer;
    public String job;
    public String address;
    public String employerNif;

    public UserFullListing(Entity user) {
        super(user.getKey().getName(),
                user.getString(USER_EMAIL),
                user.getString(USER_NAME));
        this.password = user.getString(USER_PWD);
        this.phone = user.getString(USER_PHONE);
        this.profile = user.getString(USER_PROFILE);

        this.role = user.getString(USER_ROLE);
        this.state = user.getString(USER_STATE);
        this.citizenCard = getOptionalString(user, USER_CC);
        this.userNif = getOptionalString(user, USER_NIF);
        this.employer = getOptionalString(user, USER_EMPLOYER);
        this.job = getOptionalString(user, USER_JOB);
        this.address = getOptionalString(user, USER_ADDRESS);
        this.employerNif = getOptionalString(user, USER_EMP_NIF);
    }
    // Atribui "NOT DEFINED" em caso do atributo não existir
    private String getOptionalString(Entity user, String property) {
        return user.contains(property) ? user.getString(property) : NOT_DEFINED;
    }
}
