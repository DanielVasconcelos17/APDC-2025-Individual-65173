package pt.unl.fct.di.apdc.firstwebapp.types;

public enum ProfileState {

    ATIVADA("ATIVADA"),
    SUSPENSA("SUSPENSA"),
    DESATIVADA("DESATIVADA");

    private final String type;

    ProfileState(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
