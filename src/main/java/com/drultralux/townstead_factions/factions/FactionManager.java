package com.drultralux.townstead_factions;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class FactionManager {
    private static final Map<UUID, String> PLAYER_FACTIONS = new HashMap<>();
    private static final Map<String, Set<String>> FACTION_ROSTERS = new HashMap<>();

    // Global tracking reference to mark our database instance as dirty when changes occur
    private static FactionSavedData saveInstanceRef = null;

    public static void initDatabaseInstance(ServerLevel overworld) {
        saveInstanceRef = FactionSavedData.get(overworld);
    }

    /**
     * Natively checks if the player already has a persistent faction registration entry saved.
     */
    public static boolean isPlayerAssigned(UUID playerUUID) {
        return PLAYER_FACTIONS.containsKey(playerUUID) && !PLAYER_FACTIONS.get(playerUUID).equals("None");
    }

    /**
     * Helper method to map raw NBT rows straight into our live tracking lists on world startup.
     */
    public static void loadSavedAssignment(UUID uuid, String factionName) {
        PLAYER_FACTIONS.put(uuid, factionName);
        LogManager.debug("Loaded historical database record: UUID {} -> Faction: {}", uuid, factionName);
    }

    /**
     * Handles adding active players to roster listings when they join the session.
     */
    public static void registerOnlinePlayerToRoster(ServerPlayer player) {
        if (player == null) return;
        String playerName = player.getScoreboardName();
        String assignedFaction = getPlayerFaction(player.getUUID());

        removePlayerFromAllRosters(playerName);
        if (!assignedFaction.equals("None")) {
            FACTION_ROSTERS.computeIfAbsent(assignedFaction, k -> new HashSet<>()).add(playerName);
            LogManager.debug("Player {} connected -> Mounted into active roster list for: {}", playerName, assignedFaction);
        }
    }

    /**
     * Natively registers a brand new player assignment configuration.
     */
    public static void processPlayerAssignment(ServerPlayer player, String factionName) {
        if (player == null || factionName == null) return;

        UUID playerUUID = player.getUUID();
        String playerName = player.getScoreboardName();

        PLAYER_FACTIONS.put(playerUUID, factionName);
        registerOnlinePlayerToRoster(player);

        // CRITICAL: Marks the file as dirty so NeoForge forces an immediate sector disk write on save ticks!
        if (saveInstanceRef != null) {
            saveInstanceRef.setDirty();
        }

        if (!factionName.equals("None")) {
            player.sendSystemMessage(Component.literal("§a★ Joined Faction: §e" + factionName + " §a★"));
        }
    }

    public static String getPlayerFaction(UUID playerUUID) {
        return PLAYER_FACTIONS.getOrDefault(playerUUID, "None");
    }

    public static Map<UUID, String> getAllPlayerAssignments() {
        return Collections.unmodifiableMap(PLAYER_FACTIONS);
    }

    public static Set<String> getOnlineMembers(String factionName) {
        return FACTION_ROSTERS.getOrDefault(factionName, Collections.emptySet());
    }

    public static void handlePlayerDisconnect(ServerPlayer player) {
        if (player != null) {
            removePlayerFromAllRosters(player.getScoreboardName());
        }
    }

    private static void removePlayerFromAllRosters(String playerName) {
        for (Set<String> roster : FACTION_ROSTERS.values()) {
            roster.remove(playerName);
        }
    }

    /**
     * Compares saved world records against active configuration states on player login.
     * Automatically migrates player data matrices if an administrative update causes a collision.
     */
    public static void reconcilePlayerFaction(ServerPlayer player, String activeRootID) {
        if (player == null) return;

        UUID playerUUID = player.getUUID();
        String playerName = player.getScoreboardName();

        //Calculate what their faction SHOULD be based on the live JSON config maps
        String expectedFactionName = "None";
        if (activeRootID != null) {
            expectedFactionName = ModConfig.ROOT_TO_FACTION_MAP.getOrDefault(activeRootID, "None");
        }

        //Fetch what the historical world data save files currently track them as
        boolean alreadyInDatabase = isPlayerAssigned(playerUUID);
        String historicalSavedFaction = getPlayerFaction(playerUUID);

        //Force an internal database migration if parameters don't match
        if (alreadyInDatabase && !historicalSavedFaction.equals(expectedFactionName)) {
            PLAYER_FACTIONS.put(playerUUID, expectedFactionName);

            LogManager.info("Config Collision Caught for {}! Migrated '{}' -> assigned to new group: '{}'",
                    playerName, historicalSavedFaction, expectedFactionName);

            if (saveInstanceRef != null) {
                saveInstanceRef.setDirty();
            }

            player.sendSystemMessage(Component.literal("§6★ Factions Updated! You have been migrated to: §e" + expectedFactionName + " ★"));
        }

        //Run standard online roster list registration
        registerOnlinePlayerToRoster(player);

        //If they are a new player, execute the standard initial registry path assignment
        if (!alreadyInDatabase && activeRootID != null) {
            processPlayerAssignment(player, expectedFactionName);
        } else if (!expectedFactionName.equals("None")) {
            player.sendSystemMessage(Component.literal("§a★ Active Faction: §e" + expectedFactionName + " §a★"));
        }
    }

    public static void syncFactionDataToClient(ServerPlayer player) {
        if (player == null) return;

        java.util.UUID playerUUID = player.getUUID();
        String currentFaction = getPlayerFaction(playerUUID);

        // Convert our active online roster HashSet elements into a clean serializable list structure
        List<String> onlineCompanions = new ArrayList<>(getOnlineMembers(currentFaction));

        LogManager.debug("Streaming network packet sync payload to client player: {} | Faction: {} | Online Companions Size: {}",
                player.getScoreboardName(), currentFaction, onlineCompanions.size());

        // Modern 1.21.1 NeoForge packet distribution pipeline: fires data straight over the network channel socket
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                player,
                new FactionSyncPayload(currentFaction, onlineCompanions)
        );
    }
}