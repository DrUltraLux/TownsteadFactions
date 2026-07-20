package com.drultralux.townsteadfactions.factions;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.saveddata.SavedData;
import com.drultralux.townsteadfactions.utils.LogManager;

import java.util.Map;

/**
 * Handles NBT persistence of faction data to and from the world save,
 * writing the current in-memory faction state on save and preserving the
 * raw loaded data for the server event layer to read on load.
 */
public class FactionSavedData extends SavedData {

    /** The raw NBT tag loaded from disk, kept for the server event layer to read on load. */
    public CompoundTag rawLoadedTag = new CompoundTag();

    /**
     * Loads faction save data from an NBT tag. The tag is stored as-is;
     * actual faction reconstruction happens elsewhere once the server is
     * ready to read it.
     *
     * @param tag the NBT tag loaded from disk, or {@code null} if none exists
     * @param registries the registry lookup provider, unused here but required by the {@link SavedData} API
     * @return a new {@code FactionSavedData} holding the loaded tag
     */
    public static FactionSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        FactionSavedData data = new FactionSavedData();
        if (tag != null) {
            data.rawLoadedTag = tag;
        }
        return data;
    }

    /**
     * Serializes all currently active factions into the given NBT tag,
     * including their full participant roster (players and villagers
     * alike, in one unified list) and activity log.
     *
     * @param tag the NBT tag to write faction data into
     * @param provider the registry lookup provider, unused here but required by the {@link SavedData} API
     * @return the same {@code tag}, with a {@code "factions"} entry populated
     */
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        Map<String, Faction> liveFactions = FactionManager.getInstance().getActiveFactions();

        CompoundTag factionsTag = new CompoundTag();
        liveFactions.forEach((id, faction) -> {
            if (id != null && faction != null) {
                CompoundTag factionNbt = new CompoundTag();
                factionNbt.putString("id", faction.getId());
                factionNbt.putString("displayName", faction.getDisplayName());
                factionNbt.putInt("cogs", faction.getCogs());
                factionNbt.putInt("food", faction.getFood());
                factionNbt.putInt("mana", faction.getMana());

                ListTag participantsList = new ListTag();
                faction.getParticipants().forEach((uuid, participant) -> {
                    CompoundTag participantTag = new CompoundTag();
                    participantTag.putUUID("uuid", uuid);
                    participantTag.putBoolean("isPlayer", participant.isPlayer());
                    participantTag.putBoolean("leader", participant.isLeader());

                    if (participant.isPlayer()) {
                        participantTag.putLong("joinTimestamp", participant.getJoinTimestamp());
                    } else {
                        participantTag.putString("name", participant.getCachedName());
                        participantTag.putString("rootId", participant.getCachedRootId());
                        participantTag.putString("title", participant.getCachedTitle().name());
                    }

                    participantsList.add(participantTag);
                });
                factionNbt.put("participants", participantsList);

                ListTag logList = new ListTag();
                for (ActivityLogEntry logEntry : faction.getActivityLog()) {
                    CompoundTag entryTag = new CompoundTag();
                    entryTag.putLong("timestamp", logEntry.timestamp());
                    entryTag.putString("message", logEntry.message());
                    logList.add(entryTag);
                }
                factionNbt.put("activityLog", logList);

                factionsTag.put(id, factionNbt);
            }
        });
        tag.put("factions", factionsTag);
        return tag;
    }
}