package com.drultralux.townstead_factions.client;

import com.drultralux.townstead_factions.LogManager;
import com.drultralux.townstead_factions.client.screen.KeyMappings;
import com.drultralux.townstead_factions.client.screen.FactionScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public class ClientModEvents {

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        // Simply handles opening the screen from the regular un-focused game world view state
        if (mc.player != null && mc.screen == null) {
            while (KeyMappings.OPEN_FACTION_MENU.consumeClick()) {
                LogManager.debug("Hotkey 'F' clicked! Requesting live sync from server.");
                PacketDistributor.sendToServer(new MenuRequestPayload());
                mc.setScreen(new FactionScreen());
            }
        }
    }
}