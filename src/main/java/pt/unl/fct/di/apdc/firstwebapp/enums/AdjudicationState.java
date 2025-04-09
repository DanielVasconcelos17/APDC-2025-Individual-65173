package pt.unl.fct.di.apdc.firstwebapp.enums;

public enum AdjudicationState {
    ADJUDICATED("ADJUDICATED"),
    NOT_ADJUDICATED("NOT_ADJUDICATED");

    private final String state;

    AdjudicationState(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }
}
