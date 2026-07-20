package com.drultralux.townsteadfactions.factions;

import com.drultralux.townsteadfactions.utils.LogManager;
import com.drultralux.townsteadfactions.config.ModConfig;
import com.drultralux.townsteadfactions.integration.required.OriginManager;
import com.drultralux.townsteadfactions.territory.VillageControlManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.Set;

/**
 * Manages the lifecycle of all active factions on the server: creating
 * them from configuration, loading and reconciling them with saved world
 * data, assigning players and villagers, and applying resource changes.
 *
 * <p>The sole gateway for touching {@link Faction} data — nothing else in
 * the project should hold, construct, or mutate a {@code Faction}
 * instance directly.</p>
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
                Faction factionInstance = new Faction(cleanFactionId, cleanFactionId);
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
     * state (balances, participants, etc). Falls back to a clean
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

            Faction faction = new Faction(cleanId, factionTag.getString("displayName"));
            faction.setCogs(factionTag.getInt("cogs"));
            faction.setFood(factionTag.getInt("food"));
            faction.setMana(factionTag.getInt("mana"));

            if (factionTag.contains("activityLog", 9)) { // 9 is ListTag
                ListTag logList = factionTag.getList("activityLog", 10); // 10 is CompoundTag
                for (int j = 0; j < logList.size(); j++) {
                    CompoundTag entryTag = logList.getCompound(j);
                    faction.getActivityLog().add(new ActivityLogEntry(entryTag.getLong("timestamp"), entryTag.getString("message")));
                }
            }

            if (factionTag.contains("participants", 9)) { // 9 is ListTag
                ListTag participantsList = factionTag.getList("participants", 10); // 10 is CompoundTag
                for (int j = 0; j < participantsList.size(); j++) {
                    CompoundTag participantTag = participantsList.getCompound(j);
                    try {
                        UUID uuid = participantTag.getUUID("uuid");
                        boolean isPlayer = participantTag.getBoolean("isPlayer");
                        boolean leader = participantTag.getBoolean("leader");

                        if (isPlayer) {
                            long joinTimestamp = participantTag.getLong("joinTimestamp");
                            faction.restorePlayerParticipant(uuid, joinTimestamp, leader);
                        } else {
                            String name = participantTag.getString("name");
                            String rootId = participantTag.getString("rootId");
                            FactionTitle title;
                            try {
                                title = FactionTitle.valueOf(participantTag.getString("title"));
                            } catch (Exception e) {
                                title = FactionTitle.VILLAGER;
                                LogManager.warn("Skipping unrecognized saved title for villager " + uuid + " in faction '" + cleanId + "', defaulting to Villager.");
                            }
                            faction.restoreVillagerParticipant(uuid, name, rootId, title, leader);
                        }
                    } catch (Exception e) {
                        LogManager.warn("Skipping malformed saved participant entry at index " + j + " in faction '" + cleanId + "'.");
                    }
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
                    Faction newFaction = new Faction(configFactionId, configFactionId);
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
            faction.removeParticipant(playerUUID);
        }
        this.activeFactions.get(cleanId).addPlayerParticipant(playerUUID);
        LogManager.debug("Successfully committed player " + playerUUID + " to faction instance slot: " + cleanId);
        return true;
    }

    /**
     * Adds a new villager participant to a faction, or updates their
     * cached display fields if they're already a participant there. If
     * they were previously a participant of a different faction, they're
     * removed from it first.
     *
     * @param villagerUUID the villager's UUID
     * @param factionId the faction to assign them to; does nothing if unknown
     * @param name the villager's current display name
     * @param rootId the villager's current origin (root) ID
     * @param title the villager's current resolved display title
     * @return the villager's previous faction ID, or {@code null} if they had none (a brand new registration)
     */
    public String assignVillagerToFaction(UUID villagerUUID, String factionId, String name, String rootId, FactionTitle title) {
        if (villagerUUID == null || factionId == null) return null;
        String cleanId = factionId.trim();
        Faction targetFaction = this.activeFactions.get(cleanId);
        if (targetFaction == null) {
            LogManager.warn("Attempted to assign villager " + villagerUUID + " to unknown faction ID: '" + cleanId + "'");
            return null;
        }

        String previousFactionId = getParticipantFactionId(villagerUUID);

        if (previousFactionId != null && !previousFactionId.equals(cleanId)) {
            Faction previousFaction = this.activeFactions.get(previousFactionId);
            if (previousFaction != null) {
                previousFaction.removeParticipant(villagerUUID);
            }
        }

        targetFaction.addOrUpdateVillagerParticipant(villagerUUID, name, rootId, title);
        return previousFactionId;
    }

    /**
     * Removes a participant — player or villager — from whichever faction
     * they currently belong to, if any. Used for villager death/despawn
     * cleanup.
     *
     * @param uuid the participant to remove
     */
    public static void removeParticipant(UUID uuid) {
        if (uuid == null) return;
        for (Faction faction : getInstance().getActiveFactions().values()) {
            faction.removeParticipant(uuid);
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
        return getParticipantFactionId(playerUUID);
    }

    /**
     * Finds the faction a participant — player or villager — currently
     * belongs to, by scanning all active factions' rosters.
     *
     * @param uuid the UUID of the participant to look up
     * @return their faction ID, or {@code null} if they're not in any faction
     */
    public static String getParticipantFactionId(UUID uuid) {
        if (uuid == null) return null;
        for (Faction faction : getInstance().getActiveFactions().values()) {
            if (faction.getParticipants().containsKey(uuid)) {
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
     * Binds the world save data instance used to flag pending changes for
     * disk persistence.
     *
     * @param storage the world save data instance to bind
     */
    public static void setStorageInstance(FactionSavedData storage) {
        activeStorageInstance = storage;
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
     * Returns a defensive copy of a faction's player member UUIDs (only
     * players, not villagers) by ID, so callers can't mutate the live
     * roster.
     *
     * @param factionId the faction's ID
     * @return a copy of the faction's player member UUIDs, or an empty list if it doesn't exist
     */
    public static List<UUID> getFactionMemberUUIDs(String factionId) {
        if (factionId == null) return List.of();
        Faction faction = getInstance().getActiveFactions().get(factionId.trim());
        if (faction == null) return List.of();

        List<UUID> result = new ArrayList<>();
        for (FactionParticipant participant : faction.getParticipants().values()) {
            if (participant.isPlayer()) {
                result.add(participant.getUUID());
            }
        }
        return result;
    }

    /**
     * Returns how many villagers are currently assigned to a faction.
     *
     * @param factionId the faction to count
     * @return the assigned villager count
     */
    public static int getVillagerCountForFaction(String factionId) {
        if (factionId == null) return 0;
        Faction faction = getInstance().getActiveFactions().get(factionId.trim());
        if (faction == null) return 0;

        int count = 0;
        for (FactionParticipant participant : faction.getParticipants().values()) {
            if (participant.isVillager()) count++;
        }
        return count;
    }

    /**
     * Returns every villager participant currently assigned to a faction,
     * keyed by UUID.
     *
     * @param factionId the faction to list
     * @return the assigned villagers, keyed by UUID
     */
    public static Map<UUID, FactionParticipant> getVillagersForFaction(String factionId) {
        Map<UUID, FactionParticipant> result = new HashMap<>();
        if (factionId == null) return result;
        Faction faction = getInstance().getActiveFactions().get(factionId.trim());
        if (faction == null) return result;

        for (FactionParticipant participant : faction.getParticipants().values()) {
            if (participant.isVillager()) {
                result.put(participant.getUUID(), participant);
            }
        }
        return result;
    }

    /**
     * Returns every participant — player and villager alike — currently
     * in a faction.
     *
     * @param factionId the faction to list
     * @return all of the faction's participants
     */
    public static List<FactionParticipant> getAllParticipants(String factionId) {
        if (factionId == null) return List.of();
        Faction faction = getInstance().getActiveFactions().get(factionId.trim());
        if (faction == null) return List.of();
        return new ArrayList<>(faction.getParticipants().values());
    }

    /**
     * Resolves a participant's display name for use in log messages and
     * similar text — a villager's cached name, or a player's resolved
     * username (their current online name, falling back to the server's
     * profile cache if offline).
     *
     * @param factionId the faction the participant belongs to
     * @param uuid the participant to resolve
     * @param server the server, used to resolve a player's name; may be {@code null}
     * @return the resolved display name, or the raw UUID string if it can't be resolved
     */
    public static String getParticipantDisplayName(String factionId, UUID uuid, MinecraftServer server) {
        if (factionId == null || uuid == null) return String.valueOf(uuid);
        for (FactionParticipant participant : getAllParticipants(factionId)) {
            if (participant.getUUID().equals(uuid)) {
                if (participant.isVillager()) return participant.getCachedName();
                return resolvePlayerDisplayName(server, uuid);
            }
        }
        return uuid.toString();
    }

    /**
     * Resolves a player's display name: their MCA persona name (as set in
     * the in-game player editor's General tab), if MCA data for them
     * exists — MCA's own family-tree lookup already falls back to their
     * real Minecraft account name internally if they've never customized
     * it, so this single call correctly covers both cases. Falls back
     * further to the server's online player list, then its profile
     * cache, if MCA data can't be read at all for some reason.
     *
     * @param server the server, used to look up MCA and Minecraft account data; may be {@code null}
     * @param playerUUID the player to resolve
     * @return the resolved display name, or {@code "Unknown Member"} if nothing could be resolved
     */
    public static String resolvePlayerDisplayName(MinecraftServer server, UUID playerUUID) {
        if (server == null || playerUUID == null) return "Unknown Member";

        try {
            var saveData = net.conczin.mca.server.world.data.PlayerSaveData.get(server.overworld(), playerUUID);
            String mcaName = saveData.getFamilyEntry().getName();
            if (mcaName != null && !mcaName.isBlank()) {
                return mcaName;
            }
        } catch (Exception e) {
            // MCA data unavailable for this player — fall through to the vanilla name sources below.
        }

        ServerPlayer online = server.getPlayerList().getPlayer(playerUUID);
        if (online != null) return online.getName().getString();
        var profile = server.getProfileCache().get(playerUUID);
        return profile.map(com.mojang.authlib.GameProfile::getName).orElse("Unknown Member");
    }

    /**
     * Checks whether a participant currently holds a Leader role in a
     * specific faction.
     *
     * @param factionId the faction to check
     * @param uuid the participant to check
     * @return {@code true} if they're a current leader of that faction
     */
    public static boolean isLeader(String factionId, UUID uuid) {
        if (factionId == null || uuid == null) return false;
        Faction faction = getInstance().getActiveFactions().get(factionId.trim());
        return faction != null && faction.isLeader(uuid);
    }

    /**
     * Sets whether a participant currently holds a Leader role in a
     * specific faction. Does nothing if they aren't a participant there.
     *
     * @param factionId the faction to update
     * @param uuid the participant to update
     * @param leader the new leader status
     */
    public static void setLeader(String factionId, UUID uuid, boolean leader) {
        if (factionId == null || uuid == null) return;
        Faction faction = getInstance().getActiveFactions().get(factionId.trim());
        if (faction != null) {
            faction.setLeader(uuid, leader);
            if (activeStorageInstance != null) {
                activeStorageInstance.setDirty();
            }
        }
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
     * ID, current leaders, resource balances, member count, and allowed
     * origins.
     *
     * @param factionId the faction's ID
     * @return the formatted summary, or {@code null} if the faction doesn't exist
     */
    public static String getFactionSummaryString(String factionId, MinecraftServer server) {
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

        int memberCount = 0;
        StringBuilder leadersBuilder = new StringBuilder();
        for (FactionParticipant participant : faction.getParticipants().values()) {
            memberCount++;
            if (participant.isLeader()) {
                String leaderDisplayName = participant.isPlayer()
                        ? resolvePlayerDisplayName(server, participant.getUUID())
                        : participant.getCachedName();
                leadersBuilder.append("\n §7- §f").append(leaderDisplayName)
                        .append(participant.isPlayer() ? " §7(player)" : " §7(villager)");
            }
        }
        String leadersStr = leadersBuilder.isEmpty() ? " §7None Assigned" : leadersBuilder.toString();

        return "§6=== Faction Comprehensive Review: " + faction.getDisplayName() + " ===" +
                "\n §7Internal Unique ID: §f" + faction.getId() +
                "\n §7Current Leaders:" + leadersStr +
                "\n §bBalances Matrix:" +
                "\n §7• Cogs: §f" + faction.getCogs() + " §7• Food: §f" + faction.getFood() + " §7• Mana: §f" + faction.getMana() +
                "\n §7• Power / Members Count: §f" + memberCount +
                "\n §7• Controlled Villages: §b" + VillageControlManager.getControlledVillageCount(factionId) +
                "\n §dRegistered Allowed Origins:" + originsBuilder.toString() +
                "\n §7Active Live Members Loaded: §b" + memberCount;
    }

    /**
     * Builds a formatted list of every active faction on the server, with
     * each faction's ID, display name, and player/villager counts.
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
                int memberCount = getFactionMemberUUIDs(faction.getId()).size();
                int villagerCount = getVillagerCountForFaction(faction.getId());
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