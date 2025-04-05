package pt.unl.fct.di.apdc.firstwebapp.types;

public enum ProfileState {

    ATIVADA("ACTIVATE"),
    SUSPENSA("SUSPENDED"),
    DESATIVADA("DEACTIVATE");

    private final String type;

    ProfileState(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
