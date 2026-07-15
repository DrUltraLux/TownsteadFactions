package com.drultralux.townstead_factions;

import com.drultralux.townstead_factions.client.ClientFactionCache;
import com.drultralux.townstead_factions.client.ClientModEvents;
import com.drultralux.townstead_factions.client.FactionSyncPayload;
import com.drultralux.townstead_factions.client.MenuRequestPayload;
import com.drultralux.townstead_factions.client.screen.KeyMappings;
import com.drultralux.townstead_factions.config.ModConfig;
import com.drultralux.townstead_factions.factions.FactionCommands;
import com.drultralux.townstead_factions.factions.FactionManager;
import com.drultralux.townstead_factions.roots.OriginManager;

import com.aetherianartificer.townstead.root.PlayerRoot;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(TownsteadFactions.MODID)
public class TownsteadFactions {
    public static final String MODID = "townstead_factions";

    public TownsteadFactions(IEventBus modEventBus) {

        modEventBus.addListener(this::registerPackets);
        NeoForge.EVENT_BUS.register(this);

        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            //Register explicit Key Mappings to the Mod Event Bus
            KeyMappings mappings = new KeyMappings();
            modEventBus.addListener(mappings::onRegisterKeyMappings);

            //Register client tick listener directly
            ClientModEvents clientEvents = new ClientModEvents();
            NeoForge.EVENT_BUS.register(clientEvents);
        }

        LogManager.info("Townstead Factions Initialized Successfully!");
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.level().isClientSide) {
            if (PlayerRoot.hasRoot(player)) {
                String activeRawRootID = PlayerRoot.getRootId(player);
                if (activeRawRootID != null) {
                    if (FactionManager.hasPlayerOriginShifted(player.getUUID(), activeRawRootID)) {
                        FactionManager.reconcilePlayerFaction(player, activeRawRootID);
                        FactionManager.syncFactionDataToClient(player);
                    }
                }
            }
        }
    }

    /**
     Just grab stuff at start if we need it.
     Pulling from Townstead
     Pulling from our own config file.
    */
    @SubscribeEvent
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        OriginManager.inheritTownsteadRegistries();
        ModConfig.loadModpackConfig();
    }

    /**
     Startup/fetch our faction database
     */
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        FactionManager.initDatabaseInstance(event.getServer().overworld());
        LogManager.info("World database saving frameworks attached seamlessly.");
    }

    /**
     When a player logs in, grab their current root ID.
     Might need to have this on some sort of resolve stage later to account for use of editor??
     Load/Define/Assign faction based on rootID
     */
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        try {
            if (PlayerRoot.hasRoot(player)) {
                String rawRootID = PlayerRoot.getRootId(player);
                if (rawRootID != null) {
                    FactionManager.reconcilePlayerFaction(player, rawRootID);
                    FactionManager.syncFactionDataToClient(player);
                }
            }
        } catch (Exception e) {
            LogManager.error("Failure processing login routing: ", e);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FactionManager.handlePlayerDisconnect(player);
        }
    }

    private void registerPackets(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(MODID);

        // Register Server-to-Client packet
        registrar.playToClient(
                FactionSyncPayload.TYPE,
                FactionSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    ClientFactionCache.updateCache(
                            payload.factionName(),
                            payload.rawRootID(),
                            payload.cleanOriginName(),
                            payload.onlineMembers(),
                            payload.allFactions(),
                            payload.globalOnlineCount(),
                            payload.resources()
                    );
                })
        );

        //Client-to-Server Packet
        registrar.playToServer(
                MenuRequestPayload.TYPE,
                MenuRequestPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        FactionManager.syncFactionDataToClient(serverPlayer);
                    }
                })
        );
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        FactionCommands.register(event.getDispatcher());
        LogManager.info("User faction chat commands successfully mapped onto the server engine registry.");
    }
}