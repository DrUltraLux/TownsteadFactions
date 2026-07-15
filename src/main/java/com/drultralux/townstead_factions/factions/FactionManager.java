package com.drultralux.townstead_factions.factions;

import com.drultralux.townstead_factions.LogManager;
import com.drultralux.townstead_factions.client.FactionSyncPayload;
import com.drultralux.townstead_factions.config.ModConfig;
import com.drultralux.townstead_factions.roots.OriginManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class FactionManager {
    private static final Map<UUID, String> PLAYER_FACTIONS = new HashMap<>();
    private static final Map<String, Set<String>> FACTION_ROSTERS = new HashMap<>();
    private static final Map<UUID, String> PLAYER_RAW_ORIGINS = new HashMap<>();

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

    public static boolean hasPlayerOriginShifted(UUID playerUUID, String liveRawRootID) {
        if (!PLAYER_RAW_ORIGINS.containsKey(playerUUID)) return true;
        return !PLAYER_RAW_ORIGINS.get(playerUUID).equals(liveRawRootID);
    }

    /**
     * Compares saved world records against active configuration states on player login.
     * Automatically migrates player data matrices if an administrative update causes a collision.
     */
    public static void reconcilePlayerFaction(ServerPlayer player, String activeRawRootID) {
        if (player == null || activeRawRootID == null) return;

        UUID playerUUID = player.getUUID();
        String playerName = player.getScoreboardName();

        PLAYER_RAW_ORIGINS.put(playerUUID, activeRawRootID);

        String expectedFactionName = ModConfig.ROOT_TO_FACTION_MAP.getOrDefault(activeRawRootID, "None");
        String historicalSavedFaction = PLAYER_FACTIONS.getOrDefault(playerUUID, "None");

        if (!historicalSavedFaction.equals(expectedFactionName)) {
            PLAYER_FACTIONS.put(playerUUID, expectedFactionName);
            if (saveInstanceRef != null) {
                saveInstanceRef.setDirty();
            }
        }

        removePlayerFromAllRosters(playerName);
        if (!expectedFactionName.equals("None")) {
            FACTION_ROSTERS.computeIfAbsent(expectedFactionName, k -> new HashSet<>()).add(playerName);
            if (!historicalSavedFaction.equals(expectedFactionName)) {
                String cleanOriginName = OriginManager.getCleanNameForRoot(activeRawRootID);
                player.sendSystemMessage(Component.literal("§a★ Joined Faction: §e" + expectedFactionName + " §a(" + cleanOriginName + ") ★"));
            }
        }
    }

    public static void syncFactionDataToClient(ServerPlayer player) {
        if (player == null) return;

        java.util.UUID playerUUID = player.getUUID();
        String currentFaction = getPlayerFaction(playerUUID);
        List<String> onlineCompanions = new ArrayList<>(getOnlineMembers(currentFaction));
        List<String> serverFactionsList = new ArrayList<>(com.drultralux.townstead_factions.config.ModConfig.REGISTERED_FACTIONS);

        String rawRootID = "none";
        String cleanOriginName = "None Chosen";

        // Extract raw data from Townstead natively on the server side
        if (com.aetherianartificer.townstead.root.PlayerRoot.hasRoot(player)) {
            rawRootID = com.aetherianartificer.townstead.root.PlayerRoot.getRootId(player);
            if (rawRootID != null) {
                cleanOriginName = com.drultralux.townstead_factions.roots.OriginManager.getCleanNameForRoot(rawRootID);
            }
        }

        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                player,
                new com.drultralux.townstead_factions.client.FactionSyncPayload(currentFaction, rawRootID, cleanOriginName, onlineCompanions, serverFactionsList)
        );
    }
}