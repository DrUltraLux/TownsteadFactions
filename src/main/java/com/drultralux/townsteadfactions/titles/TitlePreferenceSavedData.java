package com.drultralux.townsteadfactions.titles;

import com.drultralux.townsteadfactions.factions.FactionTitle;
import com.drultralux.townsteadfactions.utils.LogManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persists each player's self-assigned cosmetic faction title (Soldier or
 * Knight), if they've chosen one. Players with no entry here fall back to
 * their computed default title.
 */
public class TitlePreferenceSavedData extends SavedData {

    /** Self-assigned titles, keyed by player UUID. */
    public final Map<UUID, FactionTitle> selfAssignedTitles = new HashMap<>();

    /**
     * Loads title preference data from an NBT tag.
     *
     * @param tag the NBT tag loaded from disk, or {@code null} if none exists
     * @param registries the registry lookup provider, unused here but required by the {@link SavedData} API
     * @return a new {@code TitlePreferenceSavedData} populated from the tag
     */
    public static TitlePreferenceSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        TitlePreferenceSavedData data = new TitlePreferenceSavedData();
        if (tag != null && tag.contains("selfAssignedTitles", 10)) { // 10 is CompoundTag
            CompoundTag titlesTag = tag.getCompound("selfAssignedTitles");
            for (String key : titlesTag.getAllKeys()) {
                try {
                    UUID playerUUID = UUID.fromString(key);
                    FactionTitle title = FactionTitle.valueOf(titlesTag.getString(key));
                    data.selfAssignedTitles.put(playerUUID, title);
                } catch (Exception e) {
                    LogManager.warn("Skipping malformed self-assigned title entry: '" + key + "'");
                }
            }
        }
        return data;
    }

    /**
     * Serializes all self-assigned titles into the given NBT tag.
     *
     * @param tag the NBT tag to write into
     * @param provider the registry lookup provider, unused here but required by the {@link SavedData} API
     * @return the same {@code tag}, populated
     */
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag titlesTag = new CompoundTag();
        selfAssignedTitles.forEach((uuid, title) -> titlesTag.putString(uuid.toString(), title.name()));
        tag.put("selfAssignedTitles", titlesTag);
        return tag;
    }
}