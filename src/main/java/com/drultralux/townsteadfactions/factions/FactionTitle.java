package com.drultralux.townsteadfactions.factions;

public enum FactionTitle {
    // DEFAULT CONFIGURATION FALLBACKS
    LEADER("Leader"),
    SOLDIER("Soldier"),
    MEMBER("Member"),
    VILLAGER("Villager"),

    // MCA CAPITALS EXTENDED PROFILE TARGETS
    MONARCH("Monarch"),
    NOBLE("Noble"),
    KNIGHT("Knight"),
    COMMONER("Commoner");

    private final String displayName;

    FactionTitle(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }
}
