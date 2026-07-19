package com.drultralux.townsteadfactions.territory;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Persists which faction controls each known village, and whether that
 * village is currently contested (tied population between two or more
 * factions).
 */
public class VillageControlSavedData extends SavedData {

    /**
     * Per-village control state, keyed by a composite
     * {@code "dimensionId:villageIntId"} string (a plain village int ID
     * is only unique within one dimension).
     */
    public final Map<String, VillageControlState> villageStates = new HashMap<>();

    /**
     * Loads village control data from an NBT tag.
     *
     * @param tag the NBT tag loaded from disk, or {@code null} if none exists
     * @param registries the registry lookup provider, unused here but required by the {@link SavedData} API
     * @return a new {@code VillageControlSavedData} populated from the tag
     */
    public static VillageControlSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        VillageControlSavedData data = new VillageControlSavedData();
        if (tag != null && tag.contains("villages", 10)) { // 10 is CompoundTag
            CompoundTag villagesTag = tag.getCompound("villages");
            for (String key : villagesTag.getAllKeys()) {
                CompoundTag stateTag = villagesTag.getCompound(key);
                VillageControlState state = new VillageControlState();
                state.controllingFactionId = stateTag.contains("controllingFactionId", 8) ? stateTag.getString("controllingFactionId") : null; // 8 is StringTag
                state.contested = stateTag.getBoolean("contested");

                if (stateTag.contains("contestedFactions", 9)) { // 9 is ListTag
                    ListTag contestedList = stateTag.getList("contestedFactions", 8); // 8 is StringTag
                    for (int i = 0; i < contestedList.size(); i++) {
                        state.contestedFactions.add(contestedList.getString(i));
                    }
                }
                data.villageStates.put(key, state);
            }
        }
        return data;
    }

    /**
     * Serializes all village control states into the given NBT tag.
     *
     * @param tag the NBT tag to write into
     * @param provider the registry lookup provider, unused here but required by the {@link SavedData} API
     * @return the same {@code tag}, populated
     */
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag villagesTag = new CompoundTag();
        villageStates.forEach((key, state) -> {
            CompoundTag stateTag = new CompoundTag();
            if (state.controllingFactionId != null) {
                stateTag.putString("controllingFactionId", state.controllingFactionId);
            }
            stateTag.putBoolean("contested", state.contested);

            ListTag contestedList = new ListTag();
            for (String factionId : state.contestedFactions) {
                contestedList.add(StringTag.valueOf(factionId));
            }
            stateTag.put("contestedFactions", contestedList);

            villagesTag.put(key, stateTag);
        });
        tag.put("villages", villagesTag);
        return tag;
    }

    /**
     * A single village's control state: which faction (if any) currently
     * controls it, and whether it's contested.
     */
    public static class VillageControlState {

        /** The controlling faction's ID, or {@code null} if uncontrolled. */
        public String controllingFactionId;

        /** Whether this village is currently tied between two or more factions. */
        public boolean contested = false;

        /** The factions currently tied for control, only populated while {@link #contested} is true. */
        public final List<String> contestedFactions = new ArrayList<>();
    }
}