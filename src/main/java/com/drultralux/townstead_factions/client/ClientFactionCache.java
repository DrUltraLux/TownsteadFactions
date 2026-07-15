package com.drultralux.townstead_factions.client;

import java.util.ArrayList;
import java.util.List;

public class ClientFactionCache {
    private static String currentFaction = "None";
    private static String currentRawRootID = "none";
    private static String currentCleanOrigin = "None Chosen";
    private static List<String> activeRoster = new ArrayList<>();
    private static List<String> discoveredFactions = new ArrayList<>();
    private static int totalGlobalPlayers = 0;

    public static int getFactionSize() {
        return activeRoster != null ? activeRoster.size() : 0;
    }

    public static int getTotalGlobalPlayers() {
        return totalGlobalPlayers;
    }

    public static void updateCache(String faction, String rawRoot, String cleanOrigin, List<String> members, List<String> factionsList, int payloadGlobalCount) {
        currentFaction = faction != null ? faction : "None";
        currentRawRootID = rawRoot != null ? rawRoot : "none";
        currentCleanOrigin = cleanOrigin != null ? cleanOrigin : "None Chosen";
        activeRoster = members != null ? members : new ArrayList<>();
        totalGlobalPlayers = payloadGlobalCount;
        discoveredFactions = factionsList != null ? factionsList : new ArrayList<>();
    }

    public static String getCurrentFaction() { return currentFaction; }
    public static String getCurrentRawRootID() { return currentRawRootID; }
    public static String getCurrentCleanOrigin() { return currentCleanOrigin; }
    public static List<String> getActiveRoster() { return activeRoster; }
    public static List<String> getDiscoveredFactions() { return discoveredFactions; }
}
