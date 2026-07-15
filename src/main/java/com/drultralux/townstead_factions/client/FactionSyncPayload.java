package com.drultralux.townstead_factions;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

public record FactionSyncPayload(String factionName, List<String> onlineMembers) implements CustomPacketPayload {

    // Define a unique channel registration token identifier for this specific packet type
    public static final Type<FactionSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Townstead_factions.MODID, "faction_sync"));

    // The stream codec handles writing/reading the variables directly into the network layer
    public static final StreamCodec<FriendlyByteBuf, FactionSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, FactionSyncPayload::factionName,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), FactionSyncPayload::onlineMembers,
            FactionSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}