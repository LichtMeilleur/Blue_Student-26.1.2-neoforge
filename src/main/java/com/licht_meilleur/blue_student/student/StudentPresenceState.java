package com.licht_meilleur.blue_student.student;

public enum StudentPresenceState {
    UNSUMMONED("unsummoned"),
    ACTIVE("active"),
    MISSING("missing"),
    PACKED("packed"),
    RESPAWNING("respawning"),
    SLEEPING("sleeping");

    private final String key;

    StudentPresenceState(String key) {
        this.key = key;
    }

    public String asString() {
        return key;
    }

    public static StudentPresenceState fromKey(String key) {
        if (key == null) return UNSUMMONED;

        for (StudentPresenceState s : values()) {
            if (s.key.equalsIgnoreCase(key) || s.name().equalsIgnoreCase(key)) {
                return s;
            }
        }

        return UNSUMMONED;
    }
}