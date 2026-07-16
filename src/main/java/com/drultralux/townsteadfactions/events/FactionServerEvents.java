package com.drultralux.townsteadfactions.events;

import com.drultralux.townsteadfactions.LogManager;
import com.drultralux.townsteadfactions.factions.FactionCommands;
import com.drultralux.townsteadfactions.factions.FactionSavedData;
import com.drultralux.townsteadfactions.network.FactionPacketManager;
import com.drultralux.townsteadfactions.roots.OriginManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

/**
 * Centrally manages unified server-side game-loop subscriptions and player pipeline triggers.
 * Explicitly mapped via the main launchpad constructor to run safely on the NeoForge GAME bus.
 */
public class FactionServerEvents {

    /**
     * Listens for world boot triggers once dimensions are fully memory-initialized
     * to safely re-inflate stored faction states from disk files.
     *
     * @param event the structural starting event provided by the NeoForge server framework
     */
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        LogManager.info("World dimensions loaded. Running persistence worker pipeline...");
        FactionSavedData.get(event.getServer());
    }

    /**
     * Catches the command registration event to append administrative and player slash nodes.
     *
     * @param event the command registration context supplied by the server engine
     */
    @SubscribeEvent
    public static void onRegisterCommands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
        LogManager.info("Injecting player and administrative faction command routing nodes...");
        FactionCommands.register(event.getDispatcher());
    }

    /**
     * Intercepts server connection pathways to automatically route players through evaluation engines.
     *
     * @param event the player login context supplied by the server engine
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LogManager.debug("Processing login pipeline logic for player profile: " + player.getName().getString());

            // Cross-examine user attributes straight against the active Townstead Root capability maps
            OriginManager.fetchInitialRootID(player);

            // Dispatch a clean, high-capacity data matrix payload straight to their interface screen
            FactionPacketManager.sendFactionSyncPacket(player);
        }
    }
}