package com.drultralux.townsteadfactions.client;

import com.drultralux.townsteadfactions.roots.OriginManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Centrally caches synchronized faction profiles and resource indicators on the client side.
 * Configured to expose a roster map to satisfy both size and entrySet iterations across widgets.
 */
public class ClientFactionCache {
    private static String assignedFactionId = "none";
    private static final Map<String, ClientFactionData> cachedFactions = new HashMap<>();

    // Internal tracker variables for local HUD widgets
    private static int cogs = 0;
    private static int food = 0;
    private static int mana = 0;

    /**
     * Natively ingests streamed record parameters from the network layer to refresh the client overview.
     */
    public static void readSyncStream(String factionId, int syncedCogs, int syncedFood, int syncedMana, Map<String, Integer> globalFactions) {
        assignedFactionId = factionId != null ? factionId.trim() : "none";
        cogs = syncedCogs;
        food = syncedFood;
        mana = syncedMana;

        cachedFactions.clear();
        if (globalFactions != null) {
            for (Map.Entry<String, Integer> entry : globalFactions.entrySet()) {
                String id = entry.getKey();
                Integer memberCount = entry.getValue();

                if (id != null && memberCount != null) {
                    ClientFactionData data = new ClientFactionData();
                    data.id = id.trim();
                    data.name = OriginManager.getCleanName(id.trim());

                    // 💡 THE CURE: Populate as a Map to support roster.size() and roster.entrySet() concurrently!
                    for (int i = 0; i < memberCount; i++) {
                        data.roster.put(UUID.randomUUID(), "Faction Member");
                    }

                    cachedFactions.put(data.id, data);
                }
            }
        }
    }

    public static String getAssignedFactionId() {
        return assignedFactionId;
    }

    public static Map<String, ClientFactionData> getCachedFactions() {
        return cachedFactions;
    }

    public static int getCogs() { return cogs; }
    public static int getFood() { return food; }
    public static int getMana() { return mana; }

    /**
     * Embedded client data model matching your widget rendering loops exactly.
     */
    public static class ClientFactionData {
        public String id;
        public String name;
        // 💡 THE CURE: Declared as a Map to perfectly satisfy RosterDisplayWidget entry sets!
        public final Map<UUID, String> roster = new HashMap<>();
    }
}