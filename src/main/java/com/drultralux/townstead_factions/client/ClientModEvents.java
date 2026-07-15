package com.drultralux.townstead_factions;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

// CRITICAL: This annotation tells NeoForge to completely ignore this class if running on a Dedicated Server!
@EventBusSubscriber(modid = Townstead_factions.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        // Safe Client-Only Game Loop Code (like checking custom hotkey presses) lives here!
    }
}