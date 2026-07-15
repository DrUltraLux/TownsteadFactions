package com.drultralux.townstead_factions.client;

import com.drultralux.townstead_factions.Townstead_factions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MenuRequestPayload() implements CustomPacketPayload {

    public static final Type<MenuRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Townstead_factions.MODID, "menu_request"));

    // An empty stream codec since this packet passes zero extra primitive types across the wire
    public static final StreamCodec<FriendlyByteBuf, MenuRequestPayload> STREAM_CODEC = StreamCodec.unit(new MenuRequestPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}