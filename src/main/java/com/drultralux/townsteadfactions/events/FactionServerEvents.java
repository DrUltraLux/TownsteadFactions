package com.drultralux.townsteadfactions.events;

import com.drultralux.townsteadfactions.LogManager;
import com.drultralux.townsteadfactions.client.FactionSyncPayload;
import com.drultralux.townsteadfactions.factions.Faction;
import com.drultralux.townsteadfactions.factions.FactionCommands;
import com.drultralux.townsteadfactions.factions.FactionManager;
import com.drultralux.townsteadfactions.factions.FactionSavedData;
import com.drultralux.townsteadfactions.network.FactionPacketManager;
import com.drultralux.townsteadfactions.roots.OriginManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

/**
 * Centrally coordinates and houses all game framework lifecycle listeners and event tracks.
 * Completely self-contained within the events package without relying on deprecated auto-subscribers.
 */
public class FactionServerEvents {

    /**
     * Central registration hub invoked during mod initialization.
     * Sequentially attaches all local event methods onto NeoForge's gameplay bus pipeline handle.
     *
     * @param gameBus NeoForge's active game event bus reference (NeoForge.EVENT_BUS)
     */
    public static void registerListeners(IEventBus gameBus) {
        if (gameBus == null) return;
        gameBus.addListener(FactionServerEvents::onServerStarting);
        gameBus.addListener(FactionServerEvents::onRegisterCommands);
        gameBus.addListener(FactionServerEvents::onPlayerLoggedIn);
        LogManager.info("Successfully centralized and registered all faction gameplay lifecycle listeners.");
    }

    /**
     * Triggered automatically upon server environment bootstrap routines.
     * Guaranteed to process exactly once on server startup, regardless of player level locations.
     */
    public static void onServerStarting(ServerStartingEvent event) {
        LogManager.info("Server environment active. Running unified data persistence pipeline...");
        try {
            OriginManager.initializeFromTownstead();

            var storageManager = event.getServer().overworld().getDataStorage();

            FactionSavedData savedData = storageManager.computeIfAbsent(
                    new SavedData.Factory<>(
                            FactionSavedData::new,
                            FactionSavedData::load,
                            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
                    ),
                    "townsteadfactions_data"
            );

            FactionManager.setStorageInstance(savedData);

            if (savedData.rawLoadedTag != null && savedData.rawLoadedTag.contains("factions", 10)) {
                LogManager.info("Persistent world records found. Initiating secure database recovery merge...");
                FactionManager.getInstance().reconcileFactionsAndLoad(savedData.rawLoadedTag);
            } else {
                LogManager.info("No persistent history matrix found on disk. Initiating fallback configuration loader.");
                FactionManager.getInstance().reconcileFactionsAndLoad(null);
            }

        } catch (Exception e) {
            LogManager.error("Critical failure encountered during server startup data synchronization passes!", e);
        }
    }

    /**
     * Catches the command registration event to append administrative and player slash nodes.
     */
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LogManager.info("Injecting player and administrative faction command routing nodes...");
        FactionCommands.register(event.getDispatcher());
    }

    /**
     * Intercepts player login events to assign them their default faction profile
     * and sync data down network lines immediately.
     */
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LogManager.info("Player logged in: " + player.getName().getString() + ". Resolving faction parameters...");
            OriginManager.fetchInitialRootID(player);
            FactionPacketManager.sendFactionDataToClient(player);
        }
    }

    /**
     * Force-syncs an individual player's cache with the server's real data state.
     */
    public static void syncPlayerFactionData(ServerPlayer player) {
        if (player != null) {
            // Natively delegate the complete live database packet transmission downstream
            FactionPacketManager.sendFactionDataToClient(player);
        }
    }
}