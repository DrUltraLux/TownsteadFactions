package com.drultralux.townsteadfactions.factions;

import com.drultralux.townsteadfactions.utils.LogManager;
import com.drultralux.townsteadfactions.config.ModConfig;
import com.drultralux.townsteadfactions.integration.required.OriginManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages the lifecycle of all active factions on the server: creating
 * them from configuration, loading and reconciling them with saved world
 * data, assigning players, and applying resource changes.
 */
public class FactionManager {

    /** The single shared instance of this manager. */
    private static final FactionManager INSTANCE = new FactionManager();

    /** All currently active factions, keyed by faction ID. */
    private final Map<String, Faction> activeFactions = new HashMap<>();

    /** The currently bound world save data instance, or {@code null} if none is bound. */
    private static FactionSavedData activeStorageInstance = null;

    /**
     * Returns the shared {@code FactionManager} instance.
     *
     * @return the singleton instance
     */
    public static FactionManager getInstance() {
        return INSTANCE;
    }

    /**
     * Rebuilds the active factions map from {@code factions.json},
     * discarding any existing state. Factions with no valid origins are
     * skipped entirely.
     */
    public void initializeFactionsFromConfig() {
        LogManager.info("Initializing server factions from configuration mappings directly...");
        Map<String, List<String>> configMap = ModConfig.FACTIONS.getFactionsMap();
        Map<String, Faction> freshFactions = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : configMap.entrySet()) {
            String factionId = entry.getKey();
            List<String> configuredOrigins = entry.getValue();
            if (factionId == null || factionId.trim().isEmpty() || configuredOrigins == null) continue;

            String cleanFactionId = factionId.trim();
            List<String> validOrigins = new ArrayList<>();
            for (String originId : configuredOrigins) {
                if (originId != null && OriginManager.isValidOrigin(originId.trim())) {
                    validOrigins.add(originId.trim());
                }
            }

            if (!validOrigins.isEmpty()) {
                Faction factionInstance = new Faction(cleanFactionId, cleanFactionId, UUID.fromString("00000000-0000-0000-0000-000000000000"));
                factionInstance.setValidOrigins(validOrigins);
                freshFactions.put(cleanFactionId, factionInstance);
            }
        }
        this.activeFactions.clear();
        this.activeFactions.putAll(freshFactions);
    }

    /**
     * Replaces the active factions map directly with previously saved
     * faction data, bypassing config-based initialization.
     *
     * @param savedFactions the factions to load; does nothing if {@code null} or empty
     */
    public void loadFactionPersistenceData(Map<String, Faction> savedFactions) {
        if (savedFactions == null || savedFactions.isEmpty()) return;
        this.activeFactions.clear();
        this.activeFactions.putAll(savedFactions);
    }

    /**
     * Rebuilds the active factions map from saved world data, if present,
     * then reconciles it against the current {@code factions.json}
     * configuration so origin changes are picked up without losing saved
     * state (balances, members, etc). Falls back to a clean
     * config-only initialization if no save data exists.
     *
     * @param rootTag the root NBT tag loaded from the world save, or {@code null} if none exists
     */
    public void reconcileFactionsAndLoad(CompoundTag rootTag) {
        // 1. FRESH RUN FALLBACK: If no save data exists, initialize cleanly from config definitions
        if (rootTag == null || !rootTag.contains("factions", 10)) { // 10 is CompoundTag
            LogManager.info("No persistent world save data found. Executing fallback configuration loader.");
            initializeFactionsFromConfig();
            return;
        }

        // 2. LOAD SAVED DATA: Reconstruct the runtime map from the world save file
        LogManager.info("Persistent world records found. Restoring active faction data blocks from disk save...");

        this.activeFactions.clear();
        CompoundTag factionsCompound = rootTag.getCompound("factions");

        for (String factionId : factionsCompound.getAllKeys()) {
            if (factionId == null) continue;
            String cleanId = factionId.trim();
            CompoundTag factionTag = factionsCompound.getCompound(factionId);

            Faction faction = new Faction(cleanId, factionTag.getString("displayName"), new UUID(0, 0));
            faction.setCogs(factionTag.getInt("cogs"));
            faction.setFood(factionTag.getInt("food"));
            faction.setMana(factionTag.getInt("mana"));

            if (factionTag.hasUUID("leaderUUID")) {
                faction.setLeaderUUID(factionTag.getUUID("leaderUUID"));
            }

            if (factionTag.contains("members", 9)) { // 9 is ListTag
                ListTag membersList = factionTag.getList("members", 11); // 11 is IntArrayTag for UUIDs
                for (int j = 0; j < membersList.size(); j++) {
                    faction.getMembers().add(NbtUtils.loadUUID(membersList.get(j)));
                }
            }

            this.activeFactions.put(cleanId, faction);
            LogManager.debug("Loaded baseline attributes from file layer for faction profile: " + cleanId);
        }

        // 3. RECONCILE WITH LIVE CONFIGS: Overlay current origin mappings without erasing live properties
        LogManager.info("Reconciling restored database records against active config maps...");
        Map<String, List<String>> configMap = ModConfig.FACTIONS.getFactionsMap();

        if (configMap != null && !configMap.isEmpty()) {
            for (Map.Entry<String, List<String>> entry : configMap.entrySet()) {
                String configFactionId = entry.getKey().trim();
                List<String> configuredOrigins = entry.getValue();

                if (configFactionId.isEmpty() || configuredOrigins == null) continue;

                List<String> validOrigins = new ArrayList<>();
                for (String originId : configuredOrigins) {
                    if (originId != null && OriginManager.isValidOrigin(originId.trim())) {
                        validOrigins.add(originId.trim());
                    }
                }

                Faction activeFaction = this.activeFactions.get(configFactionId);
                if (activeFaction != null) {
                    // Update valid origins in case roots were moved between existing factions
                    activeFaction.setValidOrigins(validOrigins);
                    LogManager.debug("Successfully reconciled updated config origins mapping for faction: " + configFactionId);
                } else {
                    // A faction exists in config but wasn't in the save file — create it fresh
                    Faction newFaction = new Faction(configFactionId, configFactionId, UUID.fromString("00000000-0000-0000-0000-000000000000"));
                    newFaction.setValidOrigins(validOrigins);
                    this.activeFactions.put(configFactionId, newFaction);
                    LogManager.info("Config Sync: Generated a new tracking block for config entry missing from file: " + configFactionId);
                }
            }
        }
        LogManager.info("Data persistence restoration and hybrid reconciliation complete. Total active factions: " + this.activeFactions.size());
    }

    /**
     * Assigns a player to a faction, removing them from any other faction
     * first to avoid duplicate membership.
     *
     * @param playerUUID the UUID of the player to assign
     * @param factionId the ID of the faction to assign them to; does nothing if unknown
     */
    public void assignPlayerToFaction(UUID playerUUID, String factionId) {
        if (playerUUID == null || factionId == null) return;
        String cleanId = factionId.trim();

        if (this.activeFactions.containsKey(cleanId)) {
            // Remove the player from every faction first, to prevent duplicate membership
            for (Faction faction : this.activeFactions.values()) {
                faction.removeMember(playerUUID);
            }
            this.activeFactions.get(cleanId).addMember(playerUUID);
            LogManager.debug("Successfully committed player " + playerUUID + " to faction instance slot: " + cleanId);
        }
    }

    /**
     * Finds the faction a player currently belongs to, by scanning all
     * active factions' rosters.
     *
     * @param playerUUID the UUID of the player to look up
     * @return the player's faction ID, or {@code null} if they're not in any faction
     */
    public static String getPlayerFactionId(UUID playerUUID) {
        if (playerUUID == null) return null;
        for (Faction faction : getInstance().getActiveFactions().values()) {
            if (faction.getMembers().contains(playerUUID)) {
                return faction.getId();
            }
        }
        return null;
    }

    /**
     * Reads a specific resource balance from a player's faction, without
     * exposing the underlying {@link Faction} instance.
     *
     * @param playerUUID the UUID of the player whose faction to check
     * @param assetType the resource to read: {@code "cogs"}, {@code "food"}, or {@code "mana"}
     * @return the resource balance, or {@code 0} if the player has no faction or the type is unrecognized
     */
    public static int getPlayerFactionAsset(UUID playerUUID, String assetType) {
        String factionId = getPlayerFactionId(playerUUID);
        if (factionId == null) return 0;
        Faction faction = getInstance().getActiveFactions().get(factionId);
        if (faction == null) return 0;

        return switch (assetType.toLowerCase()) {
            case "cogs" -> faction.getCogs();
            case "food" -> faction.getFood();
            case "mana" -> faction.getMana();
            default -> 0;
        };
    }

    /**
     * Returns the number of members in a faction.
     *
     * @param factionId the faction's ID
     * @return the member count, or {@code 0} if the faction doesn't exist
     */
    public static int getFactionMemberCount(String factionId) {
        if (factionId == null) return 0;
        Faction faction = getInstance().getActiveFactions().get(factionId.trim());
        return (faction != null && faction.getMembers() != null) ? faction.getMembers().size() : 0;
    }

    /**
     * Binds the world save data instance used to flag pending changes for
     * disk persistence.
     *
     * @param storage the world save data instance to bind
     */
    public static void setStorageInstance(FactionSavedData storage) {
        activeStorageInstance = storage;
    }

    /**
     * Adjusts a faction's resource balance by a signed amount, clamped to
     * a minimum of zero, and flags the save data as dirty if bound.
     *
     * @param factionId the faction's ID
     * @param assetType the resource to modify: {@code "cogs"}, {@code "food"}, or {@code "mana"}
     * @param amount the amount to add (or, if negative, subtract)
     * @return {@code true} if the change was applied; {@code false} if the faction or asset type is invalid
     */
    public static boolean modifyFactionAsset(String factionId, String assetType, int amount) {
        if (factionId == null || assetType == null) return false;
        Faction faction = getInstance().getActiveFactions().get(factionId.trim());
        if (faction == null) return false;

        switch (assetType.toLowerCase()) {
            case "cogs" -> faction.setCogs(Math.max(0, faction.getCogs() + amount));
            case "food" -> faction.setFood(Math.max(0, faction.getFood() + amount));
            case "mana" -> faction.setMana(Math.max(0, faction.getMana() + amount));
            default -> { return false; }
        }

        if (activeStorageInstance != null) {
            activeStorageInstance.setDirty();
        }
        return true;
    }

    /**
     * Returns the live map of all active factions.
     *
     * @return the active factions, keyed by faction ID
     */
    public Map<String, Faction> getActiveFactions() {
        return this.activeFactions;
    }

    /**
     * Returns a faction's display name by ID, without exposing the
     * underlying {@link Faction} instance.
     *
     * @param factionId the faction's ID
     * @return the faction's display name, or {@code null} if it doesn't exist
     */
    public static String getFactionDisplayName(String factionId) {
        if (factionId == null) return null;
        Faction faction = getInstance().getActiveFactions().get(factionId.trim());
        return (faction != null) ? faction.getDisplayName() : null;
    }

    /**
     * Returns a defensive copy of a faction's member UUIDs by ID, so
     * callers can't mutate the live roster.
     *
     * @param factionId the faction's ID
     * @return a copy of the faction's member UUIDs, or an empty list if it doesn't exist
     */
    public static List<UUID> getFactionMemberUUIDs(String factionId) {
        if (factionId == null) return List.of();
        Faction faction = getInstance().getActiveFactions().get(factionId.trim());
        if (faction == null || faction.getMembers() == null) return List.of();
        return new ArrayList<>(faction.getMembers());
    }

    /**
     * Reads a specific resource balance for a faction by ID directly,
     * without exposing the underlying {@link Faction} instance.
     *
     * @param factionId the faction's ID
     * @param assetType the resource to read: {@code "cogs"}, {@code "food"}, or {@code "mana"}
     * @return the resource balance, or {@code 0} if the faction or asset type is invalid
     */
    public static int getFactionAsset(String factionId, String assetType) {
        if (factionId == null || assetType == null) return 0;
        Faction faction = getInstance().getActiveFactions().get(factionId.trim());
        if (faction == null) return 0;

        return switch (assetType.toLowerCase()) {
            case "cogs" -> faction.getCogs();
            case "food" -> faction.getFood();
            case "mana" -> faction.getMana();
            default -> 0;
        };
    }

    /**
     * Returns a defensive copy of every currently active faction's ID.
     *
     * @return the IDs of all active factions
     */
    public static Set<String> getActiveFactionIds() {
        return new HashSet<>(getInstance().getActiveFactions().keySet());
    }

    /**
     * Reads a player's assigned faction display name, without exposing the
     * underlying {@link Faction} instance.
     *
     * @param playerUUID the UUID of the player to look up
     * @return the player's faction display name, or {@code "None Assigned"} if they have none
     */
    public static String getPlayerFactionDisplayName(UUID playerUUID) {
        if (playerUUID == null) {
            return "None Assigned";
        }
        String factionId = getPlayerFactionId(playerUUID);
        if (factionId == null) {
            return "None Assigned";
        }
        Faction faction = getInstance().getActiveFactions().get(factionId.trim());
        return (faction != null) ? faction.getDisplayName() : "None Assigned";
    }

    /**
     * Applies a named resource math operation to a faction's balance,
     * clamped to a minimum of zero, and flags the save data as dirty if
     * bound.
     *
     * @param factionId the faction's ID
     * @param rawResource the resource to modify: {@code "cogs"}/{@code "cog"}, {@code "food"}, or {@code "mana"}
     * @param amount the amount to apply
     * @param operation the operation to apply: {@code "ADD"}, {@code "SUB"}, or {@code "SET"}
     * @return {@code 1} on success, {@code 0} if the faction, resource, or operation is invalid
     */
    public static int executeEncapsulatedAssetMath(String factionId, String rawResource, int amount, String operation) {
        if (factionId == null || rawResource == null || operation == null) {
            return 0;
        }

        Faction faction = getInstance().getActiveFactions().get(factionId.trim());
        if (faction == null) {
            return 0;
        }

        String targetResource = rawResource.toLowerCase().trim();
        String op = operation.toUpperCase().trim();

        switch (targetResource) {
            case "cogs", "cog" -> {
                int current = faction.getCogs();
                int next = op.equals("ADD") ? current + amount : op.equals("SUB") ? current - amount : amount;
                faction.setCogs(Math.max(0, next));
            }
            case "food" -> {
                int current = faction.getFood();
                int next = op.equals("ADD") ? current + amount : op.equals("SUB") ? current - amount : amount;
                faction.setFood(Math.max(0, next));
            }
            case "mana" -> {
                int current = faction.getMana();
                int next = op.equals("ADD") ? current + amount : op.equals("SUB") ? current - amount : amount;
                faction.setMana(Math.max(0, next));
            }
            default -> {
                return 0;
            }
        }

        if (activeStorageInstance != null) {
            activeStorageInstance.setDirty();
        }
        return 1;
    }

    /**
     * Builds a formatted, multi-line summary of a faction's full state:
     * ID, leader, resource balances, member count, and allowed origins.
     *
     * @param factionId the faction's ID
     * @return the formatted summary, or {@code null} if the faction doesn't exist
     */
    public static String getFactionSummaryString(String factionId) {
        if (factionId == null) {
            return null;
        }

        Faction faction = getInstance().getActiveFactions().get(factionId.trim());
        if (faction == null) {
            return null;
        }

        StringBuilder originsBuilder = new StringBuilder();
        if (faction.getValidOrigins() != null) {
            for (String rootId : faction.getValidOrigins()) {
                originsBuilder.append("\n §7- ").append(rootId)
                        .append(" (§e").append(OriginManager.getCleanName(rootId))
                        .append("§7)");
            }
        }

        int memberCount = (faction.getMembers() != null) ? faction.getMembers().size() : 0;
        String leaderStr = (faction.getLeaderUUID() != null) ? faction.getLeaderUUID().toString() : "None Assigned";

        return "§6=== Faction Comprehensive Review: " + faction.getDisplayName() + " ===" +
                "\n §7Internal Unique ID: §f" + faction.getId() +
                "\n §7System LeaderUUID: §f" + leaderStr +
                "\n §bBalances Matrix:" +
                "\n §7• Cogs: §f" + faction.getCogs() + " §7• Food: §f" + faction.getFood() + " §7• Mana: §f" + faction.getMana() +
                "\n §7• Power / Members Count: §f" + memberCount +
                "\n §dRegistered Allowed Origins:" + originsBuilder.toString() +
                "\n §7Active Live Members Loaded: §b" + memberCount;
    }

    /**
     * Builds a formatted list of every active faction on the server, with
     * each faction's ID, display name, and member count.
     *
     * @return the formatted list, or a placeholder message if no factions are active
     */
    public static String buildGlobalFactionListString() {
        Map<String, Faction> activeMap = getInstance().getActiveFactions();
        if (activeMap == null || activeMap.isEmpty()) {
            return "There are currently zero active live factions registered in environment mappings.";
        }

        StringBuilder listBuilder = new StringBuilder("§6=== Active Registered Server Factions (" + activeMap.size() + ") ===");
        for (Faction faction : activeMap.values()) {
            if (faction != null) {
                int memberCount = (faction.getMembers() != null) ? faction.getMembers().size() : 0;
                listBuilder.append("\n §7• §f").append(faction.getId())
                        .append(" §7-> Title: §a").append(faction.getDisplayName())
                        .append(" §7(Members: §b").append(memberCount).append("§7)");
            }
        }
        return listBuilder.toString();
    }
}