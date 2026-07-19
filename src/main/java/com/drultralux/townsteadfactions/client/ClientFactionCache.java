package com.drultralux.townsteadfactions.client;

import com.drultralux.townsteadfactions.factions.FactionTitle;
import com.drultralux.townsteadfactions.utils.LogManager;
import com.drultralux.townsteadfactions.client.screen.ScreenLayoutSaver;
import com.drultralux.townsteadfactions.config.ModConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Caches the faction data most recently synced from the server, for the
 * client-side dashboard widgets to read from. Updated either by a full
 * sync (all factions) or a delta (a single faction).
 */
public class ClientFactionCache {

    /** The faction ID assigned to the local player, or {@code "none"}. */
    private static String assignedFactionId = "none";

    /** All known factions, keyed by faction ID. */
    private static final Map<String, ClientFactionData> cachedFactions = new HashMap<>();

    /** The local player's faction's current cogs balance. */
    private static int cogs = 0;

    /** The local player's faction's current food balance. */
    private static int food = 0;

    /** The local player's faction's current mana balance. */
    private static int mana = 0;

    /**
     * Reads a full sync payload's NBT data and rebuilds this cache's
     * assigned faction, resource totals, and every known faction's data.
     *
     * @param nbt the synced faction data, or {@code null} to do nothing
     */
    public static void readSyncStream(CompoundTag nbt) {
        if (nbt == null) return;

        assignedFactionId = nbt.getString("assignedFactionId");
        cogs = nbt.getInt("cogs");
        food = nbt.getInt("food");
        mana = nbt.getInt("mana");

        cachedFactions.clear();

        if (nbt.contains("factions", 10)) { // 10 is CompoundTag
            CompoundTag factionsNbt = nbt.getCompound("factions");
            for (String id : factionsNbt.getAllKeys()) {
                if (id != null) {
                    String cleanId = id.trim();
                    cachedFactions.put(cleanId, parseFactionSnapshot(cleanId, factionsNbt.getCompound(id)));
                }
            }
        }

        int serverResetVersion = nbt.getInt("serverLayoutResetVersion");
        int lastAppliedResetVersion = ModConfig.CLIENT.getInteger("lastAppliedServerResetVersion", 0);
        if (serverResetVersion > lastAppliedResetVersion) {
            ScreenLayoutSaver.resetToDefaults();
            ScreenLayoutSaver.saveLastAppliedServerResetVersion(serverResetVersion);
            LogManager.debug("Applied server-triggered faction dashboard layout reset (version " + serverResetVersion + ").");
        }

        LogManager.debug("Client cache updated via full sync. Assigned Faction: {" + assignedFactionId + "}");
    }

    /**
     * Reads a delta payload's NBT data and updates just the single faction
     * it describes, refreshing the local player's resource totals too if
     * the update is for their own faction.
     *
     * @param nbt the delta payload data, or {@code null} to do nothing
     */
    public static void applyDelta(CompoundTag nbt) {
        if (nbt == null || !nbt.contains("faction", 10)) return; // 10 is CompoundTag

        CompoundTag factionTag = nbt.getCompound("faction");
        String id = factionTag.getString("id").trim();
        if (id.isEmpty()) return;

        ClientFactionData data = parseFactionSnapshot(id, factionTag);
        cachedFactions.put(id, data);

        if (id.equalsIgnoreCase(assignedFactionId)) {
            cogs = data.cogs;
            food = data.food;
            mana = data.mana;
        }

        LogManager.debug("Client cache applied delta update for faction: " + id);
    }

    /**
     * Parses a single faction's snapshot NBT into a {@link ClientFactionData}.
     *
     * @param id the faction's ID
     * @param factionTag the faction's snapshot data
     * @return the parsed faction data
     */
    private static ClientFactionData parseFactionSnapshot(String id, CompoundTag factionTag) {
        ClientFactionData data = new ClientFactionData();
        data.id = id;
        data.name = factionTag.contains("displayName", 8) ? factionTag.getString("displayName") : id; // 8 is StringTag
        data.cogs = factionTag.getInt("cogs");
        data.food = factionTag.getInt("food");
        data.mana = factionTag.getInt("mana");
        data.villagerCount = factionTag.getInt("villagerCount");
        data.controlledVillages = factionTag.getInt("controlledVillages");

        if (factionTag.contains("members", 9)) { // 9 is ListTag
            ListTag membersList = factionTag.getList("members", 10); // 10 is CompoundTag type identifier
            for (int i = 0; i < membersList.size(); i++) {
                CompoundTag memberTag = membersList.getCompound(i);
                if (memberTag.hasUUID("uuid")) {
                    UUID memberUuid = memberTag.getUUID("uuid");
                    String memberName = memberTag.contains("name", 8) ? memberTag.getString("name") : "Unknown Member";
                    String memberRoot = memberTag.contains("root", 8) ? memberTag.getString("root") : "Unknown";
                    String memberTitle = memberTag.contains("title", 8) ? memberTag.getString("title") : FactionTitle.MEMBER.getDisplayName();
                    data.roster.put(memberUuid, new RosterEntry(memberName, memberRoot, memberTitle));
                }
            }
        }
        if (factionTag.contains("villagerRoster", 9)) { // 9 is ListTag
            ListTag villagerList = factionTag.getList("villagerRoster", 10); // 10 is CompoundTag
            for (int i = 0; i < villagerList.size(); i++) {
                CompoundTag villagerTag = villagerList.getCompound(i);
                if (villagerTag.hasUUID("uuid")) {
                    UUID villagerUuid = villagerTag.getUUID("uuid");
                    String vName = villagerTag.contains("name", 8) ? villagerTag.getString("name") : "Unknown Villager";
                    String vRoot = villagerTag.contains("root", 8) ? villagerTag.getString("root") : "Unknown";
                    String vTitle = villagerTag.contains("title", 8) ? villagerTag.getString("title") : FactionTitle.VILLAGER.getDisplayName();
                    data.villagerRoster.put(villagerUuid, new RosterEntry(vName, vRoot, vTitle));
                }
            }
        }
        return data;
    }

    /**
     * Returns the faction ID assigned to the local player.
     *
     * @return the assigned faction ID, or {@code "none"} if unassigned
     */
    public static String getAssignedFactionId() {
        return assignedFactionId;
    }

    /**
     * Returns all factions currently known to the client.
     *
     * @return a map of faction ID to cached faction data
     */
    public static Map<String, ClientFactionData> getCachedFactions() {
        return cachedFactions;
    }

    /**
     * Returns the local player's faction's current cogs balance.
     *
     * @return the current cogs balance
     */
    public static int getCogs() { return cogs; }

    /**
     * Returns the local player's faction's current food balance.
     *
     * @return the current food balance
     */
    public static int getFood() { return food; }

    /**
     * Returns the local player's faction's current mana balance.
     *
     * @return the current mana balance
     */
    public static int getMana() { return mana; }

    /**
     * A lightweight, client-side snapshot of a single faction's identity,
     * resources, and member roster, as synced from the server.
     */
    public static class ClientFactionData {

        /** The faction's unique ID. */
        public String id;

        /** The faction's display name. */
        public String name;

        /** The faction's current cogs balance. */
        public int cogs;

        /** The faction's current food balance. */
        public int food;

        /** The faction's current mana balance. */
        public int mana;

        /** The faction's currently assigned villager population. */
        public int villagerCount;

        /** The number of villages this faction currently controls. */
        public int controlledVillages;

        /** Member roster entries, keyed by UUID. */
        public final Map<UUID, RosterEntry> roster = new HashMap<>();

        /** Villager roster entries, keyed by UUID. */
        public final Map<UUID, RosterEntry> villagerRoster = new HashMap<>();
    }

    /**
     * A single roster member's display information: their name, origin,
     * and resolved title.
     *
     * @param name the member's display name
     * @param root the member's origin display name (e.g. "High Elf"), or "Unknown"
     * @param title the member's resolved faction title (e.g. "Leader", "Sir")
     */
    public record RosterEntry(String name, String root, String title) {}
}