package pt.unl.fct.di.apdc.firstwebapp.enums;

public enum ProfileState {

    ACTIVATE("ACTIVATE"),
    SUSPENDED("SUSPENDED"),
    DEACTIVATE("DEACTIVATE");

    private final String type;

    ProfileState(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
