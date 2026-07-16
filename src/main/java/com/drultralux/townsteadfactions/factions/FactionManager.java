package com.drultralux.townsteadfactions.factions;

import com.drultralux.townsteadfactions.LogManager;
import com.drultralux.townsteadfactions.config.ModConfig;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Acts as the centralized runtime controller and memory cache for the Factions subsystem.
 * Responsible for initializing domain instances from server configurations and managing
 * active player membership lookup tables dynamically.
 */
public class FactionManager {
    private static final FactionManager INSTANCE = new FactionManager();

    // High-performance thread-safe memory mapping caches
    private final Map<String, Faction> activeFactions = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerFactionMap = new ConcurrentHashMap<>();

    private FactionManager() {
        // Enforce private construction pattern for standard singleton allocation
    }

    /**
     * Provides unified global access to the centralized faction tracking instance.
     *
     * @return the single active operational FactionManager manager reference
     */
    public static FactionManager getInstance() {
        return INSTANCE;
    }

    /**
     * Automatically constructs and initializes the core faction object instances
     * based on entries registered within the central factions configuration file.
     */
    public void initializeFactionsFromConfig() {
        LogManager.info("Loading faction registry definitions from configuration maps...");
        this.activeFactions.clear();

        List<String> structuralRegistryIds = ModConfig.FACTIONS.getFactionRegistryList();

        for (String id : structuralRegistryIds) {
            if (id == null || id.trim().isEmpty()) continue;

            String cleanId = id.trim().toLowerCase();
            // Derive a readable display name string (e.g., "solar_vanguard" -> "Solar Vanguard")
            String formattedName = formatIdToDisplayName(cleanId);

            // Set up a structural system UUID anchor for unassigned starting leadership slots
            UUID systemSystemAnchor = UUID.fromString("00000000-0000-0000-0000-000000000000");

            Faction constructedFaction = new Faction(cleanId, formattedName, systemSystemAnchor);
            this.activeFactions.put(cleanId, constructedFaction);

            LogManager.debug("Instantiated operational faction unit template: " + cleanId + " (" + formattedName + ")");
        }

        LogManager.info("Successfully mapped and cached (" + this.activeFactions.size() + ") faction domains.");
    }

    /**
     * Maps an individual player profile straight into a targeted faction membership list.
     * Automatically flushes out any outdated association traces held in memory pools.
     *
     * @param playerUUID the unique identifier string tracking the user entity
     * @param factionID the target programmatic configuration key identifier to match against
     */
    public void assignPlayerToFaction(UUID playerUUID, String factionID) {
        if (factionID == null || factionID.isEmpty()) {
            this.playerFactionMap.remove(playerUUID);
            LogManager.debug("Cleared active faction tracking indices for player: " + playerUUID);
            return;
        }

        String cleanFactionId = factionID.toLowerCase().trim();
        Faction target = this.activeFactions.get(cleanFactionId);

        if (target == null) {
            LogManager.warn("Refusing membership link process. Specified faction code '" + cleanFactionId + "' is not registered.");
            return;
        }

        // Drop historical memory trace tracking mappings if switching groups
        String oldFactionId = this.playerFactionMap.put(playerUUID, cleanFactionId);
        if (oldFactionId != null && !oldFactionId.equals(cleanFactionId)) {
            Faction oldFaction = this.activeFactions.get(oldFactionId);
            if (oldFaction != null) {
                oldFaction.removeMember(playerUUID);
            }
        }

        // Inject membership profile straight into the target object roster data layouts
        target.addOrUpdateMember(playerUUID, FactionTitle.MEMBER);
        LogManager.debug("Player " + playerUUID + " successfully indexed under faction roster: " + cleanFactionId);
    }

    /**
     * Queries the dynamic lookup table to track down which faction a player is currently sorted into.
     *
     * @param playerUUID the unique target tracking token key to locate
     * @return the string tracking identifier code of the matched faction, or null if unassigned
     */
    public String getPlayerFactionId(UUID playerUUID) {
        return this.playerFactionMap.get(playerUUID);
    }

    /**
     * Pulls the absolute live Faction object configuration reference out of the runtime storage mapping cache.
     *
     * @param factionID the clean tracking registration key string to search for
     * @return the active internal Faction class configuration profile, or null if missing
     */
    public Faction getFaction(String factionID) {
        if (factionID == null) return null;
        return this.activeFactions.get(factionID.toLowerCase().trim());
    }

    /**
     * Exposes an immutable snapshot representation tracking all populated faction entities in active memory.
     *
     * @return an unmodifiable collection tracking the server's running faction models
     */
    public Map<String, Faction> getActiveFactions() {
        return java.util.Collections.unmodifiableMap(this.activeFactions);
    }

    private String formatIdToDisplayName(String rawId) {
        String[] sentenceFragments = rawId.split("_");
        StringBuilder stringBuilder = new StringBuilder();
        for (String segment : sentenceFragments) {
            if (!segment.isEmpty()) {
                stringBuilder.append(Character.toUpperCase(segment.charAt(0)))
                        .append(segment.substring(1))
                        .append(" ");
            }
        }
        return stringBuilder.toString().trim();
    }
}