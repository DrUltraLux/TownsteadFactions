package com.drultralux.townsteadfactions.client;

import com.drultralux.townsteadfactions.TownsteadFactions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A highly resilient CustomPacketPayload that compresses all live asset states
 * into a single unified NBT CompoundTag to ensure Krypton and PacketFixer safety.
 */
public record FactionSyncPayload(CompoundTag nbtData) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FactionSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TownsteadFactions.MODID, "sync"));

    // Standard NeoForge NBT stream codec handler
    public static final StreamCodec<FriendlyByteBuf, FactionSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, FactionSyncPayload::nbtData,
            FactionSyncPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
