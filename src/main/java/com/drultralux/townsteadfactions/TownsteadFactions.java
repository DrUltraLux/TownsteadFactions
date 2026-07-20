package com.drultralux.townsteadfactions;

import com.drultralux.townsteadfactions.client.ClientFactionCache;
import com.drultralux.townsteadfactions.client.screen.ScreenLayoutSaver;
import com.drultralux.townsteadfactions.events.ClientModEvents;
import com.drultralux.townsteadfactions.config.ModConfig;
import com.drultralux.townsteadfactions.events.FactionServerEvents;
import com.drultralux.townsteadfactions.factions.FactionManager;
import com.drultralux.townsteadfactions.factions.voting.LeadershipManager;
import com.drultralux.townsteadfactions.factions.voting.VoteChoice;
import com.drultralux.townsteadfactions.network.FactionPacketActions;
import com.drultralux.townsteadfactions.network.FactionPacketDispatcher;
import com.drultralux.townsteadfactions.network.FactionPacketManager;
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

        FactionPacketDispatcher.registerC2SHandler(FactionPacketActions.FACTION_LOG_REQUEST_MORE, (player, data) ->
                FactionPacketManager.sendMoreActivityLog(player, data.getString("factionId"), data.getLong("beforeTimestamp")));

        FactionPacketDispatcher.registerC2SHandler(FactionPacketActions.FACTION_VOTE_CAST, (player, data) -> {
            String factionId = FactionManager.getPlayerFactionId(player.getUUID());
            if (factionId == null) return;
            java.util.UUID voteId = data.getUUID("voteId");
            VoteChoice choice = VoteChoice.valueOf(data.getString("choice"));
            LeadershipManager.castVote(voteId, player.getUUID(), choice, player.getServer());
            FactionPacketManager.broadcastFactionDelta(factionId, player.getServer());
        });

        FactionPacketDispatcher.registerC2SHandler(FactionPacketActions.FACTION_VOTE_REQUEST_LEADERSHIP, (player, data) -> {
            String factionId = FactionManager.getPlayerFactionId(player.getUUID());
            if (factionId == null) return;
            LeadershipManager.requestLeadership(factionId, player.getUUID(), player.getServer());
        });

        FactionPacketDispatcher.registerC2SHandler(FactionPacketActions.FACTION_LEADERSHIP_NOMINATE, (player, data) -> {
            String factionId = FactionManager.getPlayerFactionId(player.getUUID());
            if (factionId == null) return;
            java.util.UUID targetUUID = data.getUUID("targetUUID");
            com.drultralux.townsteadfactions.factions.voting.LeadershipManager.nominateForLeadership(factionId, player.getUUID(), targetUUID, player.getServer());
        });

        FactionPacketDispatcher.registerC2SHandler(FactionPacketActions.FACTION_LEADERSHIP_RESIGN, (player, data) -> {
            String factionId = FactionManager.getPlayerFactionId(player.getUUID());
            if (factionId == null) return;
            com.drultralux.townsteadfactions.factions.voting.LeadershipManager.resignLeadership(factionId, player.getUUID(), player.getServer());
        });

        FactionPacketDispatcher.registerC2SHandler(FactionPacketActions.FACTION_VILLAGE_MAP_REQUEST, (player, data) ->
                FactionPacketManager.sendVillageMap(player, data.getString("factionId"), data.getInt("index")));

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.register(ClientModEvents.ClientModBusEvents.class);
            NeoForge.EVENT_BUS.register(ClientModEvents.ClientGameBusEvents.class);
            FactionPacketDispatcher.registerS2CHandler(FactionPacketActions.FACTION_SYNC, ClientFactionCache::readSyncStream);
            FactionPacketDispatcher.registerS2CHandler(FactionPacketActions.FACTION_SYNC_DELTA, ClientFactionCache::applyDelta);
            FactionPacketDispatcher.registerS2CHandler(FactionPacketActions.FACTION_LAYOUT_RESET, data -> ScreenLayoutSaver.resetToDefaults());
            FactionPacketDispatcher.registerS2CHandler(FactionPacketActions.FACTION_LOG_MORE, ClientFactionCache::applyMoreLogHistory);
            FactionPacketDispatcher.registerS2CHandler(FactionPacketActions.FACTION_VILLAGE_MAP_RESPONSE, ClientFactionCache::applyVillageMap);
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