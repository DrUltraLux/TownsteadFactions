package com.drultralux.townsteadfactions.client;

import com.drultralux.townsteadfactions.LogManager;
import com.drultralux.townsteadfactions.client.screen.FactionScreen;
import com.drultralux.townsteadfactions.client.KeyMappings;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/**
 * Centrally coordinates system registration hooks and interface update ticks on the client side.
 * Restricts engine logic execution pathways strictly to the local physical game distribution instance.
 */
public class ClientModEvents {

    /**
     * Dedicated lifecycle listener class mapped to register mod-bus events.
     */
    public static class ClientModBusEvents {

        /**
         * Injects custom dashboard key configurations directly into the standard options registry tree.
         *
         * @param event the structural registration context supplied by the mod loading framework
         */
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            LogManager.info("Injecting faction dashboard keyboard shortcuts into user config lists...");
            event.register(KeyMappings.OPEN_FACTION_DASHBOARD);
        }
    }

    /**
     * Dedicated tick listener framework class mapping game loops to catch interface input triggers.
     */
    public static class ClientGameBusEvents {

        /**
         * Monitored execution loop tracking live controller inputs to initialize screen drawing layers.
         *
         * @param event the active runtime client frame tracking tick reference context
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