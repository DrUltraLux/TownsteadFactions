package com.drultralux.townsteadfactions.client;

import com.drultralux.townsteadfactions.LogManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Centrally coordinates localized client-side memory storage for synchronized faction state snapshots.
 * Feeds directly into MariesLib UI screen renderers to draw balances and roster tables instantly.
 */
public class ClientFactionCache {
    private static String assignedFactionId = "";
    private static final Map<String, ClientFactionData> cachedFactions = new HashMap<>();

    /**
     * Parses an incoming raw NBT packet matrix stream from the server and refreshes local client memory.
     *
     * @param streamTag the master CompoundTag compiled from the server network payload pipeline
     */
    public static void readSyncStream(CompoundTag streamTag) {
        LogManager.debug("Client cache processing incoming network data snapshot...");

        assignedFactionId = streamTag.getString("AssignedPlayerFaction");
        cachedFactions.clear();

        if (streamTag.contains("RegisteredFactionsMatrix", Tag.TAG_LIST)) {
            ListTag matrixList = streamTag.getList("RegisteredFactionsMatrix", Tag.TAG_COMPOUND);
            for (int i = 0; i < matrixList.size(); i++) {
                CompoundTag factionTag = matrixList.getCompound(i);
                String id = factionTag.getString("FactionID");
                String name = factionTag.getString("DisplayName");
                UUID leader = factionTag.getUUID("LeaderUUID");

                int cogs = factionTag.getInt("CogsBalance");
                int food = factionTag.getInt("FoodBalance");
                int mana = factionTag.getInt("ManaBalance");

                ClientFactionData localData = new ClientFactionData(id, name, leader, cogs, food, mana);

                // Parse nested roster rows for this faction
                if (factionTag.contains("SocialRoster", Tag.TAG_LIST)) {
                    ListTag rosterList = factionTag.getList("SocialRoster", Tag.TAG_COMPOUND);
                    for (int j = 0; j < rosterList.size(); j++) {
                        CompoundTag profileTag = rosterList.getCompound(j);
                        localData.addMember(
                                profileTag.getUUID("MemberUUID"),
                                profileTag.getString("TitleEnumName")
                        );
                    }
                }
                cachedFactions.put(id, localData);
            }
        }
        LogManager.debug("Client cache update complete. Cached factions count: " + cachedFactions.size());
    }

    public static String getAssignedFactionId() { return assignedFactionId; }
    public static Map<String, ClientFactionData> getCachedFactions() { return cachedFactions; }

    /**
     * Lightweight data record container mapping read-only parameters for client interface rendering layers.
     */
    public static class ClientFactionData {
        public final String id;
        public final String name;
        public final UUID leader;
        public final int cogs;
        public final int food;
        public final int mana;
        public final Map<UUID, String> roster = new HashMap<>();

        public ClientFactionData(String id, String name, UUID leader, int cogs, int food, int mana) {
            this.id = id;
            this.name = name;
            this.leader = leader;
            this.cogs = cogs;
            this.food = food;
            this.mana = mana;
        }

        public void addMember(UUID uuid, String title) {
            this.roster.put(uuid, title);
        }
    }
}