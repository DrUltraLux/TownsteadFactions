package com.drultralux.townsteadfactions.network.payload;

import com.drultralux.townsteadfactions.TownsteadFactions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A generic client-to-server payload consisting of an action name and an
 * NBT data bag. The client-to-server counterpart of
 * {@link FactionS2CPayload}; see that class for the general design.
 *
 * <p>No action currently sends this payload — it exists so future
 * client-initiated requests (e.g. a menu open request) can be added by
 * registering a handler with
 * {@link com.drultralux.townsteadfactions.network.FactionPacketDispatcher}
 * rather than creating a new payload type.</p>
 *
 * @param action the handler action name this payload should be routed to
 * @param data the payload's data, as an NBT compound tag
 */
public record FactionC2SPayload(String action, CompoundTag data) implements CustomPacketPayload {

    /** The packet type identifier used to register and dispatch this payload. */
    public static final Type<FactionC2SPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TownsteadFactions.MODID, "c2s"));

    /** Stream codec that reads and writes this payload's action name and NBT data. */
    public static final StreamCodec<FriendlyByteBuf, FactionC2SPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, FactionC2SPayload::action,
            ByteBufCodecs.COMPOUND_TAG, FactionC2SPayload::data,
            FactionC2SPayload::new
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