package com.drultralux.townsteadfactions;

import com.drultralux.townsteadfactions.client.ClientFactionCache;
import com.drultralux.townsteadfactions.client.FactionSyncPayload;
import com.drultralux.townsteadfactions.config.ModConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Main initialization class for the Townstead Factions modification.
 * Coordinates bootstrap tasks, hooks up configuration paths, and links independent event subsystems safely.
 */
@Mod(TownsteadFactions.MODID)
public class TownsteadFactions {
    public static final String MODID = "townsteadfactions";

    public static void init(IEventBus modEventBus, ModContainer container) {
        LogManager.info("Initializing launchpad sequences...");

        // Register split configurations into your environment paths
        container.registerConfig(Type.CLIENT, ModConfig.CLIENT_SPEC, "townsteadfactions-client.toml");
        container.registerConfig(Type.COMMON, ModConfig.COMMON_SPEC, "townsteadfactions-common.toml");
        container.registerConfig(Type.SERVER, ModConfig.FACTIONS_SPEC, "townsteadfactions-factions.toml");

        // Hook lifecycle tracking to refresh variables when configs load or reload
        modEventBus.addListener(TownsteadFactions::handleConfigEvent);

        // Correct registration syntax for lifecycle channels
        modEventBus.addListener((RegisterPayloadHandlersEvent event) -> {
            final PayloadRegistrar registrar = event.registrar(MODID).versioned("1.0.0");

            registrar.playToClient(
                    FactionSyncPayload.TYPE,
                    FactionSyncPayload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> {
                        LogManager.debug("Received a high-capacity data matrix packet stream from the server.");
                        ClientFactionCache.readSyncStream(payload.dataStreamTag());
                    })
            );
            LogManager.info("Network packet stream payload channels established successfully.");
        });

        LogManager.info("Launchpad sequence complete. Processing context transferred.");
    }

    private static void handleConfigEvent(final ModConfigEvent event) {
        if (event.getConfig().getModId().equals(MODID)) {
            ModConfig.onConfigLoad();
        }
    }
}