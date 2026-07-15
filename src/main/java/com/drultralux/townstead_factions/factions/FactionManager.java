package com.drultralux.townstead_factions.factions;

import com.aetherianartificer.townstead.root.PlayerRoot;
import com.drultralux.townstead_factions.LogManager;
import com.drultralux.townstead_factions.TownsteadFactions;
import com.drultralux.townstead_factions.client.FactionSyncPayload;
import com.drultralux.townstead_factions.config.ModConfig;
import com.drultralux.townstead_factions.integration.NumismaticsIntegration;
import com.drultralux.townstead_factions.roots.OriginManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

@net.neoforged.fml.common.EventBusSubscriber(modid = TownsteadFactions.MODID)
public class FactionManager {
    private static final Map<UUID, String> PLAYER_FACTIONS = new HashMap<>();
    private static final Map<String, Set<String>> FACTION_ROSTERS = new HashMap<>();
    private static final Map<UUID, String> PLAYER_RAW_ORIGINS = new HashMap<>();

    // Global tracking reference to mark our database instance as dirty when changes occur
    private static FactionSavedData saveInstanceRef = null;

    public static void initDatabaseInstance(ServerLevel overworld) {
        saveInstanceRef = FactionSavedData.get(overworld);
    }

   //seem to have forgotten about this... might need it later..
    public static boolean isPlayerAssigned(UUID playerUUID) {
        return PLAYER_FACTIONS.containsKey(playerUUID) && !PLAYER_FACTIONS.get(playerUUID).equals("None");
    }

    //Loads saved NBT data from world.
    public static void loadSavedAssignment(UUID uuid, String factionName) {
        PLAYER_FACTIONS.put(uuid, factionName);
        LogManager.debug("Loaded historical database record: UUID {} -> Faction: {}", uuid, factionName);
    }

    //Adds active players as they join
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

    //New player registration
    public static void processPlayerAssignment(ServerPlayer player, String factionName) {
        if (player == null || factionName == null) return;

        UUID playerUUID = player.getUUID();
        String playerName = player.getScoreboardName();

        PLAYER_FACTIONS.put(playerUUID, factionName);
        registerOnlinePlayerToRoster(player);

        //Marks the file as dirty so NeoForge forces an immediate sector disk write on save ticks!
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

    public static int getTotalOnlineFactionPlayers() {
        int total = 0;
        for (Set<String> roster : FACTION_ROSTERS.values()) {
            if (roster != null) {
                total += roster.size();
            }
        }
        return total;
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
        List<String> serverFactionsList = new ArrayList<>(ModConfig.REGISTERED_FACTIONS);

        String rawRootID = "none";
        String cleanOriginName = "None Chosen";

        // Extract raw data from Townstead natively on the server side
        if (PlayerRoot.hasRoot(player)) {
            rawRootID = PlayerRoot.getRootId(player);
            if (rawRootID != null) {
                cleanOriginName = OriginManager.getCleanNameForRoot(rawRootID);
            }
        }

        int totalGlobalOnline = getTotalOnlineFactionPlayers();

        FactionSavedData data = FactionSavedData.get(player.serverLevel());
        String factionID = getPlayerFaction(player.getUUID());

        int liveCogs = 0;
        int liveFood = 0;
        int liveMana = 0;

        if (factionID != null && !factionID.isEmpty()) {
            Faction faction = data.getOrCreateFaction(factionID, "Unknown Faction", player.getUUID());
            if (NumismaticsIntegration.isModPresent()) {
                // Read directly from the external mod's live currency bank account sheet ledger
                liveCogs = NumismaticsIntegration.getFactionBankAccountBalance(factionID);
            } else {
                liveCogs = faction.getCogs();
            }
            liveFood = faction.getFood();
            liveMana = faction.getMana();
        }

        // Pack your numeric fields securely inside a native CompoundTag
        CompoundTag resourceTag = new net.minecraft.nbt.CompoundTag();
        resourceTag.putInt("Cogs", liveCogs);
        resourceTag.putInt("Food", liveFood);
        resourceTag.putInt("Mana", liveMana);

        PacketDistributor.sendToPlayer(
                player,
                new FactionSyncPayload(currentFaction, rawRootID, cleanOriginName, onlineCompanions, serverFactionsList, totalGlobalOnline, resourceTag)
        );
    }

    public static void promotePlayerToLeader(ServerPlayer leader, ServerPlayer target) {
        FactionSavedData data = FactionSavedData.get(leader.serverLevel());

        String factionID = getPlayerFaction(leader.getUUID());
        if (factionID != null && !factionID.isEmpty()) {
            Faction faction = data.getOrCreateFaction(factionID, "Unknown Faction", leader.getUUID());
            if (faction.isLeader(leader.getUUID())) {
                faction.addLeader(leader.getUUID(), target.getUUID());
                data.setDirty();
                syncFactionDataToClient(leader);
                syncFactionDataToClient(target);
            }
        }
    }

    public static void resignLeadershipRole(ServerPlayer leader) {
        FactionSavedData data = FactionSavedData.get(leader.serverLevel());

        String factionID = getPlayerFaction(leader.getUUID());
        if (factionID != null && !factionID.isEmpty()) {
            Faction faction = data.getOrCreateFaction(factionID, "Unknown Faction", leader.getUUID());
            faction.resignToMember(leader.getUUID());
            data.setDirty();
            syncFactionDataToClient(leader);
        }
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onRightClickBannerPattern(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof ServerPlayer player) {
            net.minecraft.world.item.ItemStack heldItem = event.getItemStack();

            if (heldItem.getItem() instanceof net.minecraft.world.item.BannerPatternItem) {
                FactionSavedData data = FactionSavedData.get(player.serverLevel());

                // FIXED: Calls your actual static helper method from FactionSavedData
                String factionID = getPlayerFaction(player.getUUID());

                if (factionID != null && !factionID.isEmpty()) {
                    Faction faction = data.getOrCreateFaction(factionID, "Unknown Faction", player.getUUID());

                    if (faction.isLeader(player.getUUID())) {
                        CompoundTag itemNbt = (CompoundTag) heldItem.save(player.registryAccess());
                        faction.updateBanner(player.getUUID(), itemNbt);
                        data.setDirty();

                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§aSuccessfully updated your faction's banner pattern scheme!"));
                        syncFactionDataToClient(player);
                        event.setCanceled(true);
                    } else {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cOnly faction leaders can update the master banner scheme!"));
                    }
                }
            }
        }
    }
}