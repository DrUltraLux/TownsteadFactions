package com.drultralux.townstead_factions.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.List;

public record FactionSyncPayload(String factionName, String rawRootID, String cleanOriginName, List<String> onlineMembers, List<String> allFactions, int globalOnlineCount, net.minecraft.nbt.CompoundTag resources) implements CustomPacketPayload {

    public static final Type<FactionSyncPayload> TYPE = new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.drultralux.townstead_factions.Townstead_factions.MODID, "faction_sync"));

    public static final StreamCodec<FriendlyByteBuf, FactionSyncPayload> STREAM_CODEC = new StreamCodec<FriendlyByteBuf, FactionSyncPayload>() {
        @Override
        public void encode(FriendlyByteBuf buffer, FactionSyncPayload payload) {
            buffer.writeUtf(payload.factionName());
            buffer.writeUtf(payload.rawRootID());
            buffer.writeUtf(payload.cleanOriginName());
            buffer.writeCollection(payload.onlineMembers(), FriendlyByteBuf::writeUtf);
            buffer.writeCollection(payload.allFactions(), FriendlyByteBuf::writeUtf);
            buffer.writeInt(payload.globalOnlineCount());
            buffer.writeNbt(payload.resources());
        }

        @Override
        public FactionSyncPayload decode(FriendlyByteBuf buffer) {
            return new FactionSyncPayload(
                    buffer.readUtf(),
                    buffer.readUtf(),
                    buffer.readUtf(),
                    buffer.readCollection(java.util.ArrayList::new, FriendlyByteBuf::readUtf),
                    buffer.readCollection(java.util.ArrayList::new, FriendlyByteBuf::readUtf),
                    buffer.readInt(),
                    buffer.readNbt()
            );
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}