package com.drultralux.townsteadfactions.network.payload;

import com.drultralux.townsteadfactions.TownsteadFactions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A generic server-to-client payload consisting of an action name and an
 * NBT data bag. Any number of primitive values can be packed into
 * {@code data} without needing a new payload class or network
 * registration — new message types are added by picking a new
 * {@code action} name and registering a handler with
 * {@link com.drultralux.townsteadfactions.network.FactionPacketDispatcher}.
 *
 * @param action the handler action name this payload should be routed to
 * @param data the payload's data, as an NBT compound tag
 */
public record FactionS2CPayload(String action, CompoundTag data) implements CustomPacketPayload {

    /** The packet type identifier used to register and dispatch this payload. */
    public static final Type<FactionS2CPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TownsteadFactions.MODID, "s2c"));

    /** Stream codec that reads and writes this payload's action name and NBT data. */
    public static final StreamCodec<FriendlyByteBuf, FactionS2CPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, FactionS2CPayload::action,
            ByteBufCodecs.COMPOUND_TAG, FactionS2CPayload::data,
            FactionS2CPayload::new
    );

    /**
     * Returns the packet type for this payload.
     *
     * @return the {@link Type} used to identify this payload on the network
     */
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}