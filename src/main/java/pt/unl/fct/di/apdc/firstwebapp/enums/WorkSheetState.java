package pt.unl.fct.di.apdc.firstwebapp.enums;

public enum WorkSheetState {
    NOT_STARTED("NOT_STARTED"),
    IN_PROGRESS("IN_PROGRESS"),
    COMPLETED("COMPLETED");

    private final String state;

    WorkSheetState(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }

    public static WorkSheetState fromString(String state) {
        if (state == null) return null;
        for (WorkSheetState s : WorkSheetState.values()) {
            if (s.getState().equalsIgnoreCase(state))
                return s;
        }
        return null; // Se não encontrar
    }
}
