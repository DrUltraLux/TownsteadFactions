package com.drultralux.townsteadfactions.client;

import com.drultralux.townsteadfactions.TownsteadFactions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A heavy-duty, unified network data packet payload designed for NeoForge 1.21.1.
 * Packs complex multi-tier faction structural data, balances, and rosters into a single
 * highly compressed native CompoundTag byte stream to completely bypass buffer size overflows.
 */
public record FactionSyncPayload(CompoundTag dataStreamTag) implements CustomPacketPayload {

    /**
     * Unique structural channel identification resource token for network distribution routing.
     */
    public static final Type<FactionSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TownsteadFactions.MODID, "faction_sync_stream")
    );

    /**
     * High-performance stream codec optimizing data packet serialization and deserialization routines.
     */
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, FactionSyncPayload> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of(
            (buf, payload) -> buf.writeNbt(payload.dataStreamTag()),
            buf -> new FactionSyncPayload(buf.readNbt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}