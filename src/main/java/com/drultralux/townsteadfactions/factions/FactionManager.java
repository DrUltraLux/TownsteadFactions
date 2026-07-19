package com.drultralux.townsteadfactions.factions;

import com.drultralux.townsteadfactions.utils.LogManager;
import com.drultralux.townsteadfactions.config.ModConfig;
import com.drultralux.townsteadfactions.integration.required.OriginManager;
import com.drultralux.townsteadfactions.territory.VillagerFactionRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
    private final Map<String, Faction> activeFactions = new LinkedHashMap<>();

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
        Map<String, Faction> freshFactions = new LinkedHashMap<>();

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
            } else {
                LogManager.warn("Skipping faction '" + cleanFactionId + "' from config: none of its configured origins resolved to a valid Townstead root ID. Check factions.json for typos.");
            }
        }
        this.activeFactions.clear();
        this.activeFactions.putAll(freshFactions);
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

            if (factionTag.contains("activityLog", 9)) { // 9 is ListTag
                ListTag logList = factionTag.getList("activityLog", 10); // 10 is CompoundTag
                for (int j = 0; j < logList.size(); j++) {
                    CompoundTag entryTag = logList.getCompound(j);
                    faction.getActivityLog().add(new ActivityLogEntry(entryTag.getLong("timestamp"), entryTag.getString("message")));
                }
            }

            if (factionTag.contains("members", 9)) { // 9 is ListTag
                ListTag membersList = factionTag.getList("members", 10); // 10 is CompoundTag
                for (int j = 0; j < membersList.size(); j++) {
                    CompoundTag memberTag = membersList.getCompound(j);
                    UUID memberUuid = memberTag.getUUID("uuid");
                    FactionTitle savedTitle;
                    try {
                        savedTitle = FactionTitle.valueOf(memberTag.getString("title"));
                    } catch (Exception e) {
                        savedTitle = FactionTitle.MEMBER;
                        LogManager.warn("Skipping unrecognized saved title for member " + memberUuid + " in faction '" + cleanId + "', defaulting to Member.");
                    }
                    faction.restoreMember(new MemberProfile(memberUuid, savedTitle));
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
                    if (validOrigins.isEmpty()) {
                        // A faction must always have at least one valid origin. If reconciling
                        // config leaves it with none (e.g. a typo, or the origin no longer
                        // exists), prune it rather than leaving an invalid faction in place.
                        this.activeFactions.remove(configFactionId);
                        LogManager.warn("Pruned faction '" + configFactionId + "': none of its configured origins resolved to a valid Townstead root ID. Check factions.json for typos.");
                    } else {
                        // Update valid origins in case roots were moved between existing factions
                        activeFaction.setValidOrigins(validOrigins);
                        LogManager.debug("Successfully reconciled updated config origins mapping for faction: " + configFactionId);
                    }
                } else if (!validOrigins.isEmpty()) {
                    // A faction exists in config but wasn't in the save file — create it fresh
                    Faction newFaction = new Faction(configFactionId, configFactionId, UUID.fromString("00000000-0000-0000-0000-000000000000"));
                    newFaction.setValidOrigins(validOrigins);
                    this.activeFactions.put(configFactionId, newFaction);
                    LogManager.info("Config Sync: Generated a new tracking block for config entry missing from file: " + configFactionId);
                } else {
                    LogManager.warn("Skipping new faction '" + configFactionId + "' from config: none of its configured origins resolved to a valid Townstead root ID. Check factions.json for typos.");
                }
            }
        }
        LogManager.info("Data persistence restoration and hybrid reconciliation complete. Total active factions: " + this.activeFactions.size());
    }

    /**
     * Assigns a player to a faction, removing them from any other faction
     * first to avoid duplicate membership. Does nothing (and returns
     * {@code false}) if they're already correctly assigned there.
     *
     * @param playerUUID the UUID of the player to assign
     * @param factionId the ID of the faction to assign them to; does nothing if unknown
     * @return {@code true} if this call actually changed their assignment
     */
    public boolean assignPlayerToFaction(UUID playerUUID, String factionId) {
        if (playerUUID == null || factionId == null) return false;
        String cleanId = factionId.trim();

        if (!this.activeFactions.containsKey(cleanId)) {
            LogManager.warn("Attempted to assign player " + playerUUID + " to unknown faction ID: '" + cleanId + "'");
            return false;
        }

        if (cleanId.equals(getPlayerFactionId(playerUUID))) {
            return false; // already correctly assigned, nothing to do
        }

        for (Faction faction : this.activeFactions.values()) {
            faction.removeMember(playerUUID);
        }
        this.activeFactions.get(cleanId).addMember(playerUUID);
        LogManager.debug("Successfully committed player " + playerUUID + " to faction instance slot: " + cleanId);
        return true;
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
            if (faction.getMembers().containsKey(playerUUID)) {
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
        if (assetType == null) return 0;
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

        logFactionAction(factionId, "Resource '" + assetType + "' adjusted by " + amount + ".");
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
        if (faction == null) return List.of();
        return new ArrayList<>(faction.getMembers().keySet());
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
     * Returns the ID of the first configured faction, by {@code factions.json}
     * declaration order — used as a fallback destination for players whose
     * origin is valid but not explicitly assigned to any faction.
     *
     * <p>Deliberately consults {@link ModConfig#FACTIONS} rather than this
     * manager's own {@code activeFactions} map for ordering: factions
     * loaded from saved world NBT don't reliably preserve insertion order
     * (NBT compounds make no such guarantee), so only the config file
     * itself is a trustworthy source of "which faction is first."</p>
     *
     * @return the first configured faction's ID that's currently active,
     *         or {@code null} if none are (no factions configured or active)
     */
    public static String getFallbackFactionId() {
        for (String configFactionId : ModConfig.FACTIONS.getFactionsMap().keySet()) {
            String cleanId = configFactionId.trim();
            if (getInstance().getActiveFactions().containsKey(cleanId)) {
                return cleanId;
            }
        }

        // Last resort: no config-ordered match found (e.g. a faction exists only
        // in saved data with no corresponding config entry). Fall back to
        // whatever's first in the active map rather than returning null.
        var iterator = getInstance().getActiveFactions().keySet().iterator();
        return iterator.hasNext() ? iterator.next() : null;
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

        if (!op.equals("ADD") && !op.equals("SUB") && !op.equals("SET")) {
            LogManager.warn("Rejected resource math request with unrecognized operation: '" + operation + "' (expected ADD, SUB, or SET)");
            return 0;
        }

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

        logFactionAction(factionId, "Resource '" + targetResource + "' " + op + " " + amount + " (admin action).");
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
                int villagerCount = VillagerFactionRegistry.getVillagerCountForFaction(faction.getId());
                listBuilder.append("\n §7• §f").append(faction.getId())
                        .append(" §7-> Title: §a").append(faction.getDisplayName())
                        .append(" §7(Players: §b").append(memberCount)
                        .append(" §7| Villagers: §d").append(villagerCount).append("§7)");
            }
        }
        return listBuilder.toString();
    }

    /**
     * Records an entry in a faction's activity log, trimming to the
     * configured cap. The sole entry point for writing to a faction's log
     * — other classes should never touch {@link Faction#addLogEntry}
     * directly.
     *
     * @param factionId the faction to log against
     * @param message the human-readable description of what happened
     */
    public static void logFactionAction(String factionId, String message) {
        if (factionId == null || message == null) return;
        Faction faction = getInstance().getActiveFactions().get(factionId.trim());
        if (faction == null) return;

        int cap = ModConfig.COMMON.getInteger("factionActivityLogCap", 2000);
        faction.addLogEntry(message, cap);

        if (activeStorageInstance != null) {
            activeStorageInstance.setDirty();
        }
    }

    /**
     * Returns a faction's most recent activity log entries, newest first.
     *
     * @param factionId the faction to query
     * @param limit the maximum number of entries to return
     * @return the most recent entries, newest first
     */
    public static List<ActivityLogEntry> getRecentActivityLog(String factionId, int limit) {
        if (factionId == null) return List.of();
        Faction faction = getInstance().getActiveFactions().get(factionId.trim());
        if (faction == null) return List.of();

        List<ActivityLogEntry> result = new ArrayList<>();
        Iterator<ActivityLogEntry> it = faction.getActivityLog().descendingIterator();
        while (it.hasNext() && result.size() < limit) {
            result.add(it.next());
        }
        return result;
    }

    /**
     * Returns up to {@code limit} of a faction's activity log entries
     * strictly older than the given timestamp, newest first among that
     * older set — used to page further back through history.
     *
     * @param factionId the faction to query
     * @param beforeTimestamp only entries strictly older than this are returned
     * @param limit the maximum number of entries to return
     * @return the matching entries, newest first
     */
    public static List<ActivityLogEntry> getActivityLogBefore(String factionId, long beforeTimestamp, int limit) {
        if (factionId == null) return List.of();
        Faction faction = getInstance().getActiveFactions().get(factionId.trim());
        if (faction == null) return List.of();

        List<ActivityLogEntry> result = new ArrayList<>();
        Iterator<ActivityLogEntry> it = faction.getActivityLog().descendingIterator();
        while (it.hasNext() && result.size() < limit) {
            ActivityLogEntry entry = it.next();
            if (entry.timestamp() < beforeTimestamp) {
                result.add(entry);
            }
        }
        return result;
    }

    /**
     * Trims every active faction's activity log to the currently
     * configured cap, discarding the oldest entries first. Called when
     * common config is (re)loaded, so a lowered cap takes effect
     * immediately rather than waiting for each faction's next log-worthy
     * activity to catch up.
     */
    public static void trimAllActivityLogsToCap() {
        int cap = ModConfig.COMMON.getInteger("factionActivityLogCap", 2000);
        for (Faction faction : getInstance().getActiveFactions().values()) {
            faction.trimLogToCap(cap);
        }
    }
}