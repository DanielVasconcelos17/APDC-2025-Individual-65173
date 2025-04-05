package pt.unl.fct.di.apdc.firstwebapp.types;

public enum ProfileType {

    PRIVATE("private"),
    PUBLIC("public");

    private final String type;

    ProfileType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
