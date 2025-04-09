package pt.unl.fct.di.apdc.firstwebapp.enums;

public enum Role {

    ADMIN("ADMIN"),
    BACKOFFICE("BACKOFFICE"),
    ENDUSER("ENDUSER"),
    PARTNER("PARTNER");

    private final String type;
    Role(String type){
        this.type = type;
    }
    public String getType(){
        return type;
    }
}
