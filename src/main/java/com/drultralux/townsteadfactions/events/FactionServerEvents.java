package com.drultralux.townsteadfactions.events;

import com.drultralux.townsteadfactions.LogManager;
import com.drultralux.townsteadfactions.TownsteadFactions;
import com.drultralux.townsteadfactions.factions.FactionSavedData;
import com.drultralux.townsteadfactions.network.FactionPacketManager;
import com.drultralux.townsteadfactions.roots.OriginManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

/**
 * Centrally manages unified game-loop subscriptions and player pipeline triggers.
 * Automatically hooks straight into the main NeoForge FORGE event bus layer.
 */
@EventBusSubscriber(modid = TownsteadFactions.MODID)
public class FactionServerEvents {

    /**
     * Listens for world boot triggers to automatically re-inflate stored faction states from disk files.
     */
    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        LogManager.info("World lifecycle boot sequence intercepted. Launching persistence worker pipeline...");
        // This triggers FactionSavedData which automatically pulls config definitions before file inflation
        FactionSavedData.get(event.getServer());
    }

    /**
     * Intercepts server connection pathways to automatically route players through evaluation engines.
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LogManager.debug("Processing login pipeline logic for player profile: " + player.getName().getString());

            // 1. Cross-examine user attributes straight against the active Townstead Root capability maps
            OriginManager.fetchInitialRootID(player);

            // 2. Dispatch a clean, high-capacity data matrix payload straight to their interface screen
            FactionPacketManager.sendFactionSyncPacket(player);
        }
    }
}