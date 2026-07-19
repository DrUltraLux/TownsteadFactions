package com.drultralux.townsteadfactions.territory;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persists which faction each MCA villager is assigned to, along with a
 * cached display name, origin, and resolved title — captured during each
 * village census sweep, since that's the only point a live villager
 * entity is guaranteed to be available.
 */
public class VillagerFactionSavedData extends SavedData {

    /** Villager UUID to their cached faction assignment and display info. */
    public final Map<UUID, VillagerRecord> villagerRecords = new HashMap<>();

    /**
     * Loads villager records from an NBT tag.
     *
     * @param tag the NBT tag loaded from disk, or {@code null} if none exists
     * @param registries the registry lookup provider, unused here but required by the {@link SavedData} API
     * @return a new {@code VillagerFactionSavedData} populated from the tag
     */
    public static VillagerFactionSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        VillagerFactionSavedData data = new VillagerFactionSavedData();
        if (tag != null && tag.contains("villagerRecords", 10)) { // 10 is CompoundTag
            CompoundTag recordsTag = tag.getCompound("villagerRecords");
            for (String key : recordsTag.getAllKeys()) {
                try {
                    CompoundTag entryTag = recordsTag.getCompound(key);
                    VillagerRecord record = new VillagerRecord(
                            entryTag.getString("factionId"),
                            entryTag.getString("name"),
                            entryTag.getString("root"),
                            entryTag.getString("title")
                    );
                    data.villagerRecords.put(UUID.fromString(key), record);
                } catch (Exception e) {
                    // Skip malformed entries rather than fail the whole load
                }
            }
        }
        return data;
    }

    /**
     * Serializes all villager records into the given NBT tag.
     *
     * @param tag the NBT tag to write into
     * @param provider the registry lookup provider, unused here but required by the {@link SavedData} API
     * @return the same {@code tag}, populated
     */
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag recordsTag = new CompoundTag();
        villagerRecords.forEach((uuid, record) -> {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("factionId", record.factionId());
            entryTag.putString("name", record.name());
            entryTag.putString("root", record.root());
            entryTag.putString("title", record.title());
            recordsTag.put(uuid.toString(), entryTag);
        });
        tag.put("villagerRecords", recordsTag);
        return tag;
    }

    /**
     * A villager's cached faction assignment and roster display info, as
     * of the last census sweep that visited their village.
     *
     * @param factionId the villager's assigned faction
     * @param name the villager's display name
     * @param root the villager's origin display name (e.g. "High Elf")
     * @param title the villager's resolved title (e.g. "Soldier", "Villager")
     */
    public record VillagerRecord(String factionId, String name, String root, String title) {}
}