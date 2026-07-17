package com.drultralux.townsteadfactions.client;

import com.drultralux.townsteadfactions.LogManager;
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
     * Ingests the network packet's unified NBT compound to safely refresh client data matrices.
     */
    public static void readSyncStream(net.minecraft.nbt.CompoundTag nbt) {
        if (nbt == null) return;

        //Extract local player resource values using our specific NBT keys
        assignedFactionId = nbt.getString("assignedFactionId");
        cogs = nbt.getInt("cogs");
        food = nbt.getInt("food");
        mana = nbt.getInt("mana");

        cachedFactions.clear();

        if (nbt.contains("globalFactionRosterCounts", 10)) {
            net.minecraft.nbt.CompoundTag rosterNbt = nbt.getCompound("globalFactionRosterCounts");
            for (String id : rosterNbt.getAllKeys()) {
                int memberCount = rosterNbt.getInt(id);

                if (id != null) {
                    ClientFactionData data = new ClientFactionData();
                    data.id = id.trim();
                    data.name = com.drultralux.townsteadfactions.roots.OriginManager.getCleanName(id.trim());
                    for (int i = 0; i < memberCount; i++) {
                        data.roster.put(UUID.randomUUID(), "Faction Member");
                    }

                    cachedFactions.put(data.id, data);
                }
            }
        }

        LogManager.debug("Client cache updated successfully via NBT block. Assigned Faction: {"+assignedFactionId+"}");    }

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
        public final Map<UUID, String> roster = new HashMap<>();
    }
}
