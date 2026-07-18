package com.drultralux.townsteadfactions.events;

import com.drultralux.townsteadfactions.utils.LogManager;
import com.drultralux.townsteadfactions.factions.FactionCommands;
import com.drultralux.townsteadfactions.factions.FactionManager;
import com.drultralux.townsteadfactions.factions.FactionSavedData;
import com.drultralux.townsteadfactions.network.FactionPacketManager;
import com.drultralux.townsteadfactions.integration.required.OriginManager;
import com.drultralux.townsteadfactions.layout.LayoutResetManager;
import com.drultralux.townsteadfactions.layout.LayoutResetSavedData;
import com.drultralux.townsteadfactions.network.FactionPacketActions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

/**
 * Registers and handles all server-side lifecycle events for Townstead
 * Factions: server startup data loading, command registration, and player
 * login syncing.
 */
public class FactionServerEvents {

    /**
     * Registers all of this class's event listeners onto the given event
     * bus. Should be called once during mod initialization.
     *
     * @param gameBus the event bus to register listeners on (typically {@code NeoForge.EVENT_BUS})
     */
    public static void registerListeners(IEventBus gameBus) {
        if (gameBus == null) return;
        gameBus.addListener(FactionServerEvents::onServerStarting);
        gameBus.addListener(FactionServerEvents::onRegisterCommands);
        gameBus.addListener(FactionServerEvents::onPlayerLoggedIn);
        LogManager.info("Successfully centralized and registered all faction gameplay lifecycle listeners.");
    }

    /**
     * Runs once when the server starts: initializes origin data, loads
     * saved faction data from disk (or falls back to defaults if none
     * exists), and hands it off to the {@link FactionManager}.
     *
     * @param event the server starting event
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

            LayoutResetSavedData layoutResetData = storageManager.computeIfAbsent(
                    new SavedData.Factory<>(
                            LayoutResetSavedData::new,
                            LayoutResetSavedData::load,
                            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
                    ),
                    "townsteadfactions_layoutreset"
            );
            LayoutResetManager.setStorageInstance(layoutResetData);

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
     * Registers faction-related commands with the server's command
     * dispatcher.
     *
     * @param event the command registration event
     */
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LogManager.info("Injecting player and administrative faction command routing nodes...");
        FactionCommands.register(event.getDispatcher());
    }

    /**
     * Resolves a newly logged-in player's faction origin and sends them
     * their current faction data.
     *
     * @param event the player login event
     */
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LogManager.info("Player logged in: " + player.getName().getString() + ". Resolving faction parameters...");
            OriginManager.fetchInitialRootID(player);
            FactionPacketManager.sendFactionDataToClient(player);
            if (LayoutResetManager.consumePendingReset(player.getUUID())) {
                FactionPacketManager.sendToPlayer(player, FactionPacketActions.FACTION_LAYOUT_RESET, new CompoundTag());
            }
        }
    }

    /**
     * Sends a player their current faction data, refreshing their
     * client-side cache to match the server's state.
     *
     * @param player the player to sync, or {@code null} to do nothing
     */
    public static void syncPlayerFactionData(ServerPlayer player) {
        if (player != null) {
            FactionPacketManager.sendFactionDataToClient(player);
        }
    }
}