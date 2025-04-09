package pt.unl.fct.di.apdc.firstwebapp.enums;

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

    public static ProfileType fromString(String type) {
        if (type == null) return null;
        for (ProfileType profile : ProfileType.values()) {
            if (profile.getType().equalsIgnoreCase(type))
                return profile;
        }
        return null; // Se não encontrar
    }
}
