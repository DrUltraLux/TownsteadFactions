package com.drultralux.townsteadfactions.territory;

import com.drultralux.townsteadfactions.territory.VillagerFactionSavedData.VillagerRecord;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The sole gateway for villager-to-faction assignments and their cached
 * roster display info. Mirrors {@code FactionManager}'s ownership
 * pattern.
 */
public final class VillagerFactionRegistry {

    /** The currently bound world save data instance, or {@code null} if none is bound. */
    private static VillagerFactionSavedData activeStorageInstance = null;

    private VillagerFactionRegistry() {}

    /**
     * Binds the world save data instance backing this registry's state.
     *
     * @param storage the world save data instance to bind
     */
    public static void setStorageInstance(VillagerFactionSavedData storage) {
        activeStorageInstance = storage;
    }

    /**
     * Returns a villager's assigned faction.
     *
     * @param villagerUUID the villager to look up
     * @return the villager's assigned faction ID, or {@code null} if unassigned
     */
    public static String getVillagerFaction(UUID villagerUUID) {
        VillagerRecord record = getRecord(villagerUUID);
        return (record != null) ? record.factionId() : null;
    }

    /**
     * Returns a villager's full cached record.
     *
     * @param villagerUUID the villager to look up
     * @return the villager's record, or {@code null} if unassigned
     */
    public static VillagerRecord getRecord(UUID villagerUUID) {
        if (activeStorageInstance == null || villagerUUID == null) return null;
        return activeStorageInstance.villagerRecords.get(villagerUUID);
    }

    /**
     * Assigns (or reassigns) a villager to a faction and updates their
     * cached roster display info.
     *
     * @param villagerUUID the villager to assign
     * @param record the villager's faction, name, origin, and title
     */
    public static void assignVillager(UUID villagerUUID, VillagerRecord record) {
        if (activeStorageInstance == null || villagerUUID == null || record == null) return;
        activeStorageInstance.villagerRecords.put(villagerUUID, record);
        activeStorageInstance.setDirty();
    }

    /**
     * Removes a villager's record, e.g. if they die or despawn, so the
     * registry doesn't grow unbounded over a long-running server's
     * lifetime.
     *
     * @param villagerUUID the villager to remove
     */
    public static void removeVillager(UUID villagerUUID) {
        if (activeStorageInstance == null || villagerUUID == null) return;
        if (activeStorageInstance.villagerRecords.remove(villagerUUID) != null) {
            activeStorageInstance.setDirty();
        }
    }

    /**
     * Counts how many villagers are currently assigned to a faction.
     *
     * @param factionId the faction to count
     * @return the assigned villager count
     */
    public static int getVillagerCountForFaction(String factionId) {
        if (activeStorageInstance == null || factionId == null) return 0;
        int count = 0;
        for (VillagerRecord record : activeStorageInstance.villagerRecords.values()) {
            if (factionId.equals(record.factionId())) count++;
        }
        return count;
    }

    /**
     * Returns every villager record currently assigned to a faction,
     * keyed by villager UUID.
     *
     * @param factionId the faction to list
     * @return the assigned villagers' records
     */
    public static Map<UUID, VillagerRecord> getVillagersForFaction(String factionId) {
        Map<UUID, VillagerRecord> result = new HashMap<>();
        if (activeStorageInstance == null || factionId == null) return result;
        activeStorageInstance.villagerRecords.forEach((uuid, record) -> {
            if (factionId.equals(record.factionId())) {
                result.put(uuid, record);
            }
        });
        return result;
    }
}