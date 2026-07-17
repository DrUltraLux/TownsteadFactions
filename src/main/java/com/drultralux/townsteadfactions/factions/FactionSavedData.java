package com.drultralux.townsteadfactions.factions;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.Map;
import java.util.UUID;

/**
 * Centrally manages the binary NBT persistence layer serialization for live faction profiles.
 * Flushes active currency balances and membership registers directly down to world files on save checkpoints.
 */
public class FactionSavedData extends SavedData {
    public CompoundTag rawLoadedTag = new CompoundTag();

    /**
     * Natively loads the raw NBT data from disk on world boot.
     * Acts as a pure, stateless reader file wrapper.
     */
    public static FactionSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        FactionSavedData data = new FactionSavedData();
        if (tag != null) {
            // Cache the raw tag safely inside the instance container so our server event layer can read it
            data.rawLoadedTag = tag;
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        // Read directly from the live runtime memory registry immediately before disk write
        Map<String, Faction> liveFactions = FactionManager.getInstance().getActiveFactions();

        CompoundTag factionsTag = new CompoundTag();
        liveFactions.forEach((id, faction) -> {
            if (id != null && faction != null) {
                CompoundTag factionNbt = new CompoundTag();
                factionNbt.putString("id", faction.getId());
                factionNbt.putString("displayName", faction.getDisplayName());
                if (faction.getLeaderUUID() != null) {
                    factionNbt.putUUID("leaderUUID", faction.getLeaderUUID());
                }
                factionNbt.putInt("cogs", faction.getCogs());
                factionNbt.putInt("food", faction.getFood());
                factionNbt.putInt("mana", faction.getMana());

                ListTag membersList = new ListTag();
                for (UUID memberUuid : faction.getMembers()) {
                    if (memberUuid != null) {
                        membersList.add(NbtUtils.createUUID(memberUuid));
                    }
                }
                factionNbt.put("members", membersList);

                factionsTag.put(id, factionNbt);
            }
        });
        tag.put("factions", factionsTag);
        return tag;
    }
}