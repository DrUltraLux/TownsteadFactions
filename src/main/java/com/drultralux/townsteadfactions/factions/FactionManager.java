package com.drultralux.townsteadfactions.factions;

import com.drultralux.townsteadfactions.LogManager;
import com.drultralux.townsteadfactions.config.ModConfig;
import com.drultralux.townsteadfactions.roots.OriginManager;
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
     * Loops through factions.json, filters out unmapped origins, and populates live Faction instances.
     * Enforces the constraint that a faction must have at least one valid origin to be initialized.
     */
    public void initializeFactionsFromConfig() {
        LogManager.info("Initializing server factions from configuration mappings...");

        // 1. Fetch the raw configuration dictionary map directly from your dynamic JSON container
        Map<String, List<String>> configMap = ModConfig.FACTIONS.getFactionsMap();
        Map<String, Faction> reconsiledFactions = new HashMap<>();

        // 2. MAIN CREATION LOOP: Process each config category row dynamically
        for (Map.Entry<String, List<String>> entry : configMap.entrySet()) {
            String factionId = entry.getKey();
            List<String> configuredOrigins = entry.getValue();

            if (factionId == null || factionId.trim().isEmpty() || configuredOrigins == null) {
                continue;
            }

            // Clean up the casing structure to maintain uniform layout lookups
            String cleanFactionId = factionId.trim();
            List<String> validOrigins = new ArrayList<>();

            // 🔍 CROSS-EXAMINE LAYER: Check configured items against Townstead's active registry
            for (String originId : configuredOrigins) {
                if (originId != null) {
                    String targetKey = originId.trim();
                    if (OriginManager.isValidOrigin(targetKey)) {
                        validOrigins.add(targetKey);
                    } else {
                        LogManager.warn("Config tracking verification warning: Origin '" + targetKey + "' was not found in Townstead registry cache.");
                    }
                }
            }

            if (!validOrigins.isEmpty()) {
                // If the faction already exists from a previous server save file data block, preserve its state
                Faction factionInstance;
                if (this.activeFactions.containsKey(cleanFactionId)) {
                    factionInstance = this.activeFactions.get(cleanFactionId);
                    LogManager.debug("Re-initializing existing live faction instance: " + cleanFactionId);
                } else {
                    // Create a pristine new Faction tracking block using the default system UUID anchor
                    LogManager.info("Creating fresh Faction instance from config layout target -> " + cleanFactionId);
                    factionInstance = new Faction(cleanFactionId, cleanFactionId, UUID.fromString("00000000-0000-0000-0000-000000000000"));
                }

                // Synchronize the verified, clean origins array list straight to the Faction object structure
                factionInstance.setValidOrigins(validOrigins);
                reconsiledFactions.put(cleanFactionId, factionInstance);
            } else {
                LogManager.warn("Skipping initialization for faction '" + cleanFactionId + "': Possesses zero valid Townstead origins.");
            }
        }

        // 3. ADMIN REMOVAL LOOP: Clear out any active tracking frames dropped or renamed out of the JSON file
        this.activeFactions.clear();
        this.activeFactions.putAll(reconsiledFactions);

        LogManager.info("Faction initialization complete. Total active structures verified and loaded: " + this.activeFactions.size());
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

    public Map<String, Faction> getActiveFactions() {
        return this.activeFactions;
    }
}