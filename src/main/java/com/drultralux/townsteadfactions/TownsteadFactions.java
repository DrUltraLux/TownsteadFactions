package com.drultralux.townsteadfactions;

import com.drultralux.townsteadfactions.client.ClientFactionCache;
import com.drultralux.townsteadfactions.client.ClientModEvents;
import com.drultralux.townsteadfactions.client.FactionSyncPayload;
import com.drultralux.townsteadfactions.config.ModConfig;
import com.drultralux.townsteadfactions.events.FactionServerEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Main initialization constructor launchpad class for the Townstead Factions modification.
 * Coordinates bootstrap tasks, hooks up nested configuration paths, and links independent event subsystems safely.
 */
@Mod("townsteadfactions")
public class TownsteadFactions {
    /**
     * Unique text workspace key representing the mod namespace inside registries.
     */
    public static final String MODID = "townsteadfactions";

    /**
     * Modern NeoForge 1.21.1 constructor entrypoint executed dynamically by the mod loader during boot.
     * Registers nested configuration subfolders and maps event tracking channels safely.
     *
     * @param modEventBus the specific event pipeline managing mod setup tasks
     * @param container the context configuration tracking this mod's metadata records
     */
    public TownsteadFactions(IEventBus modEventBus, ModContainer container) {
        LogManager.info("Initializing launchpad sequences via mod constructor...");

        container.registerConfig(Type.CLIENT, ModConfig.CLIENT_SPEC, "townsteadfactions/client.toml");
        container.registerConfig(Type.COMMON, ModConfig.COMMON_SPEC, "townsteadfactions/common.toml");
        //container.registerConfig(Type.COMMON, ModConfig.FACTIONS_SPEC, "townsteadfactions/factions.toml");

        // Hook lifecycle tracking to safely run onConfigLoad ONLY after files are verified on disk
        modEventBus.addListener(this::handleConfigEvent);

        // Correct registration syntax for lifecycle channels
        modEventBus.addListener((RegisterPayloadHandlersEvent event) -> {
            final PayloadRegistrar registrar = event.registrar(MODID).versioned("1.0.0");

            registrar.playToClient(
                    FactionSyncPayload.TYPE,
                    FactionSyncPayload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> {
                        LogManager.debug("Received a high-capacity data matrix packet stream from the server.");
                        ClientFactionCache.readSyncStream(payload.factionId(), payload.cogs(), payload.food(), payload.mana(), payload.globalFactions());
                    })
            );
            LogManager.info("Network packet stream payload channels established successfully.");
        });

        FactionServerEvents.registerListeners(NeoForge.EVENT_BUS);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.register(ClientModEvents.ClientModBusEvents.class);
            NeoForge.EVENT_BUS.register(ClientModEvents.ClientGameBusEvents.class);
        }

        LogManager.info("Launchpad constructor sequence complete. Processing context transferred.");
    }

    /**
     * Handles background config refresh operations when variables change or finish loading on the disk.
     * Safely executes processing tasks without triggering early instantiation crashes.
     *
     * @param event the active configuration update context event
     */
    private void handleConfigEvent(final ModConfigEvent event) {
        if (event.getConfig().getModId().equals(MODID)) {
            ModConfig.onConfigLoad();
        }
    }
}