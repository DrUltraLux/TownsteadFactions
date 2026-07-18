package com.drultralux.townsteadfactions.events;

import com.drultralux.townsteadfactions.client.KeyMappings;
import com.drultralux.townsteadfactions.utils.LogManager;
import com.drultralux.townsteadfactions.client.screen.FactionPalette;
import com.drultralux.townsteadfactions.client.screen.FactionScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/**
 * Registers client-only event handlers for Townstead Factions: key
 * mapping registration, client setup, and per-tick hotkey handling.
 */
public class ClientModEvents {

    /**
     * Handles mod-bus events fired during client startup, such as key
     * binding and palette registration.
     */
    public static class ClientModBusEvents {

        /**
         * Registers the faction dashboard key binding so it appears in the
         * controls menu and can be triggered in-game.
         *
         * @param event the key mapping registration event
         */
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            LogManager.info("Injecting faction dashboard keyboard shortcuts into user config lists...");
            event.register(KeyMappings.OPEN_FACTION_DASHBOARD);
        }

        /**
         * Initializes the faction UI color palette during client setup.
         *
         * @param event the client setup event
         */
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            FactionPalette.init();
        }
    }

    /**
     * Handles per-tick game-bus events on the client, such as detecting the
     * dashboard hotkey.
     */
    public static class ClientGameBusEvents {

        /**
         * Checks each client tick whether the faction dashboard hotkey was
         * pressed while no other screen is open, and opens the faction
         * screen if so.
         *
         * @param event the client tick event
         */
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();

            if (mc.player == null || mc.screen != null) {
                return;
            }

            while (KeyMappings.OPEN_FACTION_DASHBOARD.consumeClick()) {
                LogManager.debug("Hotkey 'O' clicked! Requesting live sync from server.");
                mc.setScreen(new FactionScreen());
            }
        }
    }
}