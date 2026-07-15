package com.drultralux.townstead_factions;

import java.util.ArrayList;
import java.util.List;

public class ClientFactionCache {
    private static String currentFaction = "None";
    private static List<String> activeRoster = new ArrayList<>();

    public static void updateCache(String faction, List<String> members) {
        currentFaction = faction != null ? faction : "None";
        activeRoster = members != null ? members : new ArrayList<>();

        LogManager.debug("Client cache refreshed via server packet sync! Current Faction: {}", currentFaction);
    }

    public static String getCurrentFaction() { return currentFaction; }
    public static List<String> getActiveRoster() { return activeRoster; }
}
