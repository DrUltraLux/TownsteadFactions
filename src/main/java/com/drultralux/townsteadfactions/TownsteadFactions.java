package com.drultralux.townsteadfactions;

import com.drultralux.townsteadfactions.client.ClientFactionCache;
import com.drultralux.townsteadfactions.client.screen.ScreenLayoutSaver;
import com.drultralux.townsteadfactions.events.ClientModEvents;
import com.drultralux.townsteadfactions.config.ModConfig;
import com.drultralux.townsteadfactions.events.FactionServerEvents;
import com.drultralux.townsteadfactions.network.FactionPacketActions;
import com.drultralux.townsteadfactions.network.FactionPacketDispatcher;
import com.drultralux.townsteadfactions.network.payload.FactionC2SPayload;
import com.drultralux.townsteadfactions.network.payload.FactionS2CPayload;
import com.drultralux.townsteadfactions.utils.LogManager;
import net.minecraft.server.level.ServerPlayer;
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
 * The main mod entry point for Townstead Factions. Registers config
 * specs, network payload handlers, and client/server event listeners.
 */
@Mod("townsteadfactions")
public class TownsteadFactions {

    /** This mod's unique namespace ID, used throughout registries and resource locations. */
    public static final String MODID = "townsteadfactions";

    /**
     * Constructed by NeoForge during mod loading. Registers this mod's
     * configs, network payloads, and event listeners.
     *
     * @param modEventBus the mod-specific event bus used for setup-time events
     * @param container this mod's container, used to register configuration files
     */
    public TownsteadFactions(IEventBus modEventBus, ModContainer container) {
        LogManager.info("Initializing launchpad sequences via mod constructor...");

        container.registerConfig(Type.CLIENT, ModConfig.CLIENT_SPEC, "townsteadfactions/client.toml");
        container.registerConfig(Type.COMMON, ModConfig.COMMON_SPEC, "townsteadfactions/common.toml");
        //container.registerConfig(Type.COMMON, ModConfig.FACTIONS_SPEC, "townsteadfactions/factions.toml");

        // Only handle config-loaded events once files are verified on disk
        modEventBus.addListener(this::handleConfigEvent);

        modEventBus.addListener((RegisterPayloadHandlersEvent event) -> {
            final PayloadRegistrar registrar = event.registrar(MODID).versioned("1.0.0");

            registrar.playToClient(
                    FactionS2CPayload.TYPE,
                    FactionS2CPayload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() ->
                            FactionPacketDispatcher.dispatchS2C(payload.action(), payload.data()))
            );

            registrar.playToServer(
                    FactionC2SPayload.TYPE,
                    FactionC2SPayload.STREAM_CODEC,
                    (payload, context) -> context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer serverPlayer) {
                            FactionPacketDispatcher.dispatchC2S(serverPlayer, payload.action(), payload.data());
                        }
                    })
            );

            LogManager.info("Network packet stream payload channels established successfully.");
        });

        FactionServerEvents.registerListeners(NeoForge.EVENT_BUS);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.register(ClientModEvents.ClientModBusEvents.class);
            NeoForge.EVENT_BUS.register(ClientModEvents.ClientGameBusEvents.class);
            FactionPacketDispatcher.registerS2CHandler(FactionPacketActions.FACTION_SYNC, ClientFactionCache::readSyncStream);
            FactionPacketDispatcher.registerS2CHandler(FactionPacketActions.FACTION_SYNC_DELTA, ClientFactionCache::applyDelta);
            FactionPacketDispatcher.registerS2CHandler(FactionPacketActions.FACTION_LAYOUT_RESET, data -> ScreenLayoutSaver.resetToDefaults());
        }

        LogManager.info("Launchpad constructor sequence complete. Processing context transferred.");
    }

    /**
     * Reloads this mod's runtime config values whenever its config file
     * changes or finishes loading.
     *
     * @param event the config event; ignored unless it's for this mod
     */
    private void handleConfigEvent(final ModConfigEvent event) {
        if (event.getConfig().getModId().equals(MODID)) {
            ModConfig.onConfigLoad();
        }
    }
}