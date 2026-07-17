package com.drultralux.townsteadfactions.client;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.HashMap;
import java.util.Map;

/**
 * Type-safe NeoForge network payload record tracking faction balances and global membership counts.
 * Reconfigured to stream explicit parameters directly instead of using a raw CompoundTag wrapper.
 */
public record FactionSyncPayload(String factionId, int cogs, int food, int mana, Map<String, Integer> globalFactions) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FactionSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("townsteadfactions", "sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FactionSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUtf(payload.factionId());
                buf.writeInt(payload.cogs());
                buf.writeInt(payload.food());
                buf.writeInt(payload.mana());
                buf.writeInt(payload.globalFactions().size());
                payload.globalFactions().forEach((id, count) -> {
                    buf.writeUtf(id);
                    buf.writeInt(count);
                });
            },
            buf -> {
                String factionId = buf.readUtf();
                int cogs = buf.readInt();
                int food = buf.readInt();
                int mana = buf.readInt();
                int size = buf.readInt();
                Map<String, Integer> map = new HashMap<>();
                for (int i = 0; i < size; i++) {
                    map.put(buf.readUtf(), buf.readInt());
                }
                return new FactionSyncPayload(factionId, cogs, food, mana, map);
            }
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
