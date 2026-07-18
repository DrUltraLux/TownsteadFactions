package com.drultralux.townsteadfactions.network;

import com.drultralux.townsteadfactions.utils.LogManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Central routing table for {@code FactionS2CPayload} and
 * {@code FactionC2SPayload} messages. Features register a handler once
 * for a given action name; incoming payloads are routed to that handler
 * by matching their {@code action} field.
 *
 * <p>This avoids needing a new {@code CustomPacketPayload} class and a new
 * {@code PayloadRegistrar} registration for every message type — adding a
 * new message is: pick an action name (see {@link FactionPacketActions}),
 * register a handler here, and send data tagged with that action.</p>
 */
public final class FactionPacketDispatcher {

    /** Registered handlers for server-to-client payloads, keyed by action name. */
    private static final Map<String, Consumer<CompoundTag>> S2C_HANDLERS = new HashMap<>();

    /** Registered handlers for client-to-server payloads, keyed by action name. */
    private static final Map<String, BiConsumer<ServerPlayer, CompoundTag>> C2S_HANDLERS = new HashMap<>();

    private FactionPacketDispatcher() {}

    /**
     * Registers a handler for an incoming server-to-client payload action.
     * Intended to be called once, client-side only, during mod setup.
     *
     * @param action the action name to handle
     * @param handler the handler to invoke with the payload's data
     */
    public static void registerS2CHandler(String action, Consumer<CompoundTag> handler) {
        S2C_HANDLERS.put(action, handler);
    }

    /**
     * Registers a handler for an incoming client-to-server payload action.
     * Intended to be called once, server-side, during mod setup.
     *
     * @param action the action name to handle
     * @param handler the handler to invoke with the sending player and the payload's data
     */
    public static void registerC2SHandler(String action, BiConsumer<ServerPlayer, CompoundTag> handler) {
        C2S_HANDLERS.put(action, handler);
    }

    /**
     * Routes an incoming server-to-client payload to its registered
     * handler, if one exists.
     *
     * @param action the payload's action name
     * @param data the payload's data
     */
    public static void dispatchS2C(String action, CompoundTag data) {
        Consumer<CompoundTag> handler = S2C_HANDLERS.get(action);
        if (handler != null) {
            handler.accept(data);
        } else {
            LogManager.warn("Received S2C faction packet with unregistered action: " + action);
        }
    }

    /**
     * Routes an incoming client-to-server payload to its registered
     * handler, if one exists.
     *
     * @param player the player who sent the payload
     * @param action the payload's action name
     * @param data the payload's data
     */
    public static void dispatchC2S(ServerPlayer player, String action, CompoundTag data) {
        BiConsumer<ServerPlayer, CompoundTag> handler = C2S_HANDLERS.get(action);
        if (handler != null) {
            handler.accept(player, data);
        } else {
            LogManager.warn("Received C2S faction packet with unregistered action: " + action);
        }
    }
}