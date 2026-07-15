package com.drultralux.townstead_factions.client;

import com.drultralux.townstead_factions.Townstead_factions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

public record FactionSyncPayload(String factionName, String rawRootID, String cleanOriginName, List<String> onlineMembers, List<String> allFactions) implements CustomPacketPayload {

    public static final Type<FactionSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Townstead_factions.MODID, "faction_sync"));

    public static final StreamCodec<FriendlyByteBuf, FactionSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, FactionSyncPayload::factionName,
            ByteBufCodecs.STRING_UTF8, FactionSyncPayload::rawRootID,
            ByteBufCodecs.STRING_UTF8, FactionSyncPayload::cleanOriginName,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), FactionSyncPayload::onlineMembers,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), FactionSyncPayload::allFactions,
            FactionSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}