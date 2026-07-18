package com.drultralux.townsteadfactions.layout;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Persists the state needed to drive faction dashboard layout resets: a
 * global reset counter bumped by an admin-triggered reset-all, and a set
 * of players with a targeted reset still pending delivery (queued while
 * they were offline).
 */
public class LayoutResetSavedData extends SavedData {

    /** Bumped each time an admin triggers a reset for all players. */
    public int globalResetVersion = 0;

    /** UUIDs of players with a targeted reset queued for their next login. */
    public final Set<UUID> pendingPlayerResets = new HashSet<>();

    /**
     * Loads layout reset data from an NBT tag.
     *
     * @param tag the NBT tag loaded from disk, or {@code null} if none exists
     * @param registries the registry lookup provider, unused here but required by the {@link SavedData} API
     * @return a new {@code LayoutResetSavedData} populated from the tag
     */
    public static LayoutResetSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LayoutResetSavedData data = new LayoutResetSavedData();
        if (tag != null) {
            data.globalResetVersion = tag.getInt("globalResetVersion");

            if (tag.contains("pendingPlayerResets", 9)) { // 9 is ListTag
                ListTag pendingList = tag.getList("pendingPlayerResets", 11); // 11 is IntArrayTag for UUIDs
                for (int i = 0; i < pendingList.size(); i++) {
                    data.pendingPlayerResets.add(NbtUtils.loadUUID(pendingList.get(i)));
                }
            }
        }
        return data;
    }

    /**
     * Serializes the current global reset version and pending player set
     * into the given NBT tag.
     *
     * @param tag the NBT tag to write into
     * @param provider the registry lookup provider, unused here but required by the {@link SavedData} API
     * @return the same {@code tag}, populated
     */
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt("globalResetVersion", this.globalResetVersion);

        ListTag pendingList = new ListTag();
        for (UUID uuid : this.pendingPlayerResets) {
            pendingList.add(NbtUtils.createUUID(uuid));
        }
        tag.put("pendingPlayerResets", pendingList);

        return tag;
    }
}