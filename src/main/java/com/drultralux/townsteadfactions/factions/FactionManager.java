package com.drultralux.townsteadfactions.factions;

import com.drultralux.townsteadfactions.LogManager;
import com.drultralux.townsteadfactions.config.ModConfig;
import com.drultralux.townsteadfactions.roots.OriginManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Centrally manages the creation, initialization, and tracking of active Faction instances.
 * Enforces the config file as the single source of truth for structural definitions.
 */
public class FactionManager {
    private static final FactionManager INSTANCE = new FactionManager();

    /** Runtime map tracking all live active faction instances by their unique identifier names. */
    private final Map<String, Faction> activeFactions = new HashMap<>();

    public static FactionManager getInstance() {
        return INSTANCE;
    }

    /**
     * Initializes factions by loading mappings from factions.json.
     * Validates origins, merges data with existing loaded factions to preserve state,
     * and updates the active factions map.
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
     * Direct Persistence Restorer: Replaces running tracking maps directly with the data read from the world save files.
     */
    public void loadFactionPersistenceData(Map<String, Faction> savedFactions) {
        if (savedFactions == null || savedFactions.isEmpty()) return;
        this.activeFactions.clear();
        this.activeFactions.putAll(savedFactions);
    }

    /**
     * Parent Orchestrator: Ingests raw world file NBT tags and reconstructs the server-side active factions map.
     * Natively executes the fallback configuration loop if the world save data is empty.
     * Reconciles and synchronizes live storage assets with current configuration origin maps if data exists.
     */
    public void reconcileFactionsAndLoad(net.minecraft.nbt.CompoundTag rootTag) {
        // 1. FRESH RUN FALLBACK: If no save data exists, initialize cleanly from config definitions
        if (rootTag == null || !rootTag.contains("factions", 10)) { // 10 is CompoundTag
            LogManager.info("No persistent world save data found. Executing fallback configuration loader.");
            initializeFactionsFromConfig();
            return;
        }

        // 2. LOAD SAVED RECURSION DATA: Reconstruct the runtime map exclusively from the world save file
        LogManager.info("Persistent world records found. Restoring active faction data blocks from disk save...");

        this.activeFactions.clear();
        net.minecraft.nbt.CompoundTag factionsCompound = rootTag.getCompound("factions");

        for (String factionId : factionsCompound.getAllKeys()) {
            if (factionId == null) continue;
            String cleanId = factionId.trim();
            net.minecraft.nbt.CompoundTag factionTag = factionsCompound.getCompound(factionId);

            // Instantiate the Faction object with its persistent historical metrics intact
            Faction faction = new Faction(cleanId, factionTag.getString("displayName"), new java.util.UUID(0,0));
            faction.setCogs(factionTag.getInt("cogs"));
            faction.setFood(factionTag.getInt("food"));
            faction.setMana(factionTag.getInt("mana"));

            if (factionTag.hasUUID("leaderUUID")) {
                faction.setLeaderUUID(factionTag.getUUID("leaderUUID"));
            }

            if (factionTag.contains("members", 9)) { // 9 is ListTag
                net.minecraft.nbt.ListTag membersList = factionTag.getList("members", 11); // 11 is IntArrayTag for UUIDs
                for (int j = 0; j < membersList.size(); j++) {
                    faction.getMembers().add(net.minecraft.nbt.NbtUtils.loadUUID(membersList.get(j)));
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

                // Process and filter valid origins from the updated JSON mappings
                List<String> validOrigins = new java.util.ArrayList<>();
                for (String originId : configuredOrigins) {
                    if (originId != null && OriginManager.isValidOrigin(originId.trim())) {
                        validOrigins.add(originId.trim());
                    }
                }

                Faction activeFaction = this.activeFactions.get(configFactionId);
                if (activeFaction != null) {
                    // Update the valid origins array—this catches instances where roots moved around inside existing IDs!
                    activeFaction.setValidOrigins(validOrigins);
                    LogManager.debug("Successfully reconciled updated config origins mapping for faction: " + configFactionId);
                } else {
                    // Fallback: If a faction exists in config but was completely missing from the save file, initialize it fresh
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
     * Binds a player to an explicit faction instance profile by updating membership registries.
     */
    public void assignPlayerToFaction(UUID playerUUID, String factionId) {
        if (playerUUID == null || factionId == null) return;
        String cleanId = factionId.trim();

        if (this.activeFactions.containsKey(cleanId)) {
            // Unbind player from all alternative faction structures first to prevent duplication collisions
            for (Faction faction : this.activeFactions.values()) {
                faction.removeMember(playerUUID);
            }
            // Attach the member directly to their newly resolved home target container
            this.activeFactions.get(cleanId).addMember(playerUUID);
            LogManager.debug("Successfully committed player " + playerUUID + " to faction instance slot: " + cleanId);
        }
    }

    /**
     * Finds the faction ID that a player belongs to by scanning active runtime faction rosters.
     */
    public static String getPlayerFactionId(UUID playerUUID) {
        if (playerUUID == null) return null;
        for (Faction faction : getInstance().getActiveFactions().values()) {
            if (faction.getMembers().contains(playerUUID)) {
                return faction.getId(); // Assuming getMembers() and getId() exist on Faction
            }
        }
        return null;
    }

    /**
     * Gets a specific asset balance for a player's faction without exposing the Faction instance.
     * assetType can be "cogs", "food", or "mana".
     */
    public static int getPlayerFactionAsset(java.util.UUID playerUUID, String assetType) {
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
     * Gets the total member count of a specific faction by its unique identifier string.
     */
    public static int getFactionMemberCount(String factionId) {
        if (factionId == null) return 0;
        Faction faction = getInstance().getActiveFactions().get(factionId.trim());
        return (faction != null && faction.getMembers() != null) ? faction.getMembers().size() : 0;
    }

    // Holds the transient reference to the active world save data session
    private static FactionSavedData activeStorageInstance = null;

    /**
     * Binds the current world level data storage instance to the live runtime memory layout.
     */
    public static void setStorageInstance(FactionSavedData storage) {
        activeStorageInstance = storage;
    }

    /**
     * Modifies a specific asset type for a faction by its string ID without exposing the Faction instance.
     * @param assetType Can be "cogs", "food", or "mana"
     * @param amount The integer amount to add (positive) or remove (negative)
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

        // Flag the storage layer as dirty natively if our static tracker is bound
        if (activeStorageInstance != null) {
            activeStorageInstance.setDirty();
        }
        return true;
    }

    public Map<String, Faction> getActiveFactions() {
        return this.activeFactions;
    }

    /**
     * Safely reads a player's assigned faction display name using their raw account UUID identifier.
     * Prevents external code classes from querying or leaking the underlying Faction instance structure.
     */
    public static String getPlayerFactionDisplayName(java.util.UUID playerUUID) {
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
     * Executes resource arithmetic operations securely inside the manager context.
     * Prevents negative balances and automatically flags the active storage session as dirty.
     * @return 1 on successful execution, 0 on total operational failure.
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

        // Automatically dispatch the disk-flushing flag if our tracker is initialized
        if (activeStorageInstance != null) {
            activeStorageInstance.setDirty();
        }
        return 1;
    }

    /**
     * Constructs a comprehensive review text matrix for a specified faction identifier.
     * Extracts values internally and formats them with Minecraft color symbols cleanly.
     * @return The complete compiled display string, or null if the faction cannot be matched.
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
                        .append(" (§e").append(com.drultralux.townsteadfactions.roots.OriginManager.getCleanName(rootId))
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
     * Iterates through the entire active tracking registry to assemble a structured
     * global listing index of all server factions currently loaded in system memory.
     */
    public static String buildGlobalFactionListString() {
        java.util.Map<String, Faction> activeMap = getInstance().getActiveFactions();
        if (activeMap == null || activeMap.isEmpty()) {
            return "There are currently zero active live factions registered in environment mappings.";
        }

        java.lang.StringBuilder listBuilder = new java.lang.StringBuilder("§6=== Active Registered Server Factions (" + activeMap.size() + ") ===");
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