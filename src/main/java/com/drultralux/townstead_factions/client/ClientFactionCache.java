package com.drultralux.townstead_factions.client;

import java.util.ArrayList;
import java.util.List;

public class ClientFactionCache {
    private static String currentFaction = "None";
    private static String currentRawRootID = "none";
    private static String currentCleanOrigin = "None Chosen";
    private static List<String> activeRoster = new ArrayList<>();
    private static List<String> discoveredFactions = new ArrayList<>();

    public static void updateCache(String faction, String rawRoot, String cleanOrigin, List<String> members, List<String> factionsList) {
        currentFaction = faction != null ? faction : "None";
        currentRawRootID = rawRoot != null ? rawRoot : "none";
        currentCleanOrigin = cleanOrigin != null ? cleanOrigin : "None Chosen";
        activeRoster = members != null ? members : new ArrayList<>();
        discoveredFactions = factionsList != null ? factionsList : new ArrayList<>();
    }

    public static String getCurrentFaction() { return currentFaction; }
    public static String getCurrentRawRootID() { return currentRawRootID; }
    public static String getCurrentCleanOrigin() { return currentCleanOrigin; }
    public static List<String> getActiveRoster() { return activeRoster; }
    public static List<String> getDiscoveredFactions() { return discoveredFactions; }
}
