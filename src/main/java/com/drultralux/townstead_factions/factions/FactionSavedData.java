package com.drultralux.townstead_factions.factions;

import com.drultralux.townstead_factions.LogManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FactionSavedData extends SavedData {
    private static final String DATA_NAME = "townstead_factions_players";
    private final Map<String, Faction> activeFactions = new HashMap<>();

    // Standard factory definition required by modern 1.21.1 NeoForge SavedData setups
    public static final SavedData.Factory<FactionSavedData> FACTORY = new SavedData.Factory<>(
            FactionSavedData::new,
            FactionSavedData::load,
            null // DataFixerType is null for basic custom mods
    );

    public FactionSavedData() {
        // Constructor for fresh world data generation structures
    }

    public Faction getFaction(String factionID) {
        return activeFactions.get(factionID);
    }

    public Faction getOrCreateFaction(String factionID, String cleanName, java.util.UUID creatorUUID) {
        if (!activeFactions.containsKey(factionID)) {
            Faction faction = new Faction(factionID, cleanName, creatorUUID);
            activeFactions.put(factionID, faction);
            this.setDirty();
            return faction;
        }
        return activeFactions.get(factionID);
    }

    /**
     * Automatically reads the compiled NBT data straight off the system disk during world loading loops.
     */
    public static FactionSavedData load(CompoundTag nbt, HolderLookup.Provider provider) {
        FactionSavedData data = new FactionSavedData();

        if (nbt.contains("Assignments", 9)) { // 9 is the NBT Type code for ListTag arrays
            ListTag list = nbt.getList("Assignments", 10); // 10 is the NBT Compound Tag code

            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                try {
                    UUID uuid = UUID.fromString(entry.getString("UUID"));
                    String faction = entry.getString("Faction");

                    // Directly mount the saved database record straight into our running FactionManager cache!
                    FactionManager.loadSavedAssignment(uuid, faction);
                } catch (IllegalArgumentException e) {
                    LogManager.error("Failed to parse saved player UUID token inside database rows", e);
                }
            }
        }

        if (nbt.contains("FactionsDataList", 9)) {
            ListTag fList = nbt.getList("FactionsDataList", 10);
            for (int i = 0; i < fList.size(); i++) {
                Faction f = new Faction(fList.getCompound(i));
                data.activeFactions.put(f.getFactionID(), f);
            }
        }

        return data;
    }

    /**
     * Automatically converts active in-memory factions maps into clean binary files when the world autosaves.
     */
    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider provider) {
        ListTag list = new ListTag();

        // Loop over the active live FactionManager dataset matrix rows
        for (Map.Entry<UUID, String> entry : FactionManager.getAllPlayerAssignments().entrySet()) {
            CompoundTag compound = new CompoundTag();
            compound.putString("UUID", entry.getKey().toString());
            compound.putString("Faction", entry.getValue());
            list.add(compound);
        }

        nbt.put("Assignments", list);

        net.minecraft.nbt.ListTag fList = new ListTag();
        for (Faction f : activeFactions.values()) {
            fList.add(f.saveToNBT());
        }
        nbt.put("FactionsDataList", fList);
        return nbt;
    }

    /**
     * Dynamic helper to fetch or construct this master save controller straight from the server instance.
     */
    public static FactionSavedData get(ServerLevel level) {
        DimensionDataStorage storage = level.getServer().overworld().getDataStorage();
        return storage.computeIfAbsent(FACTORY, DATA_NAME);
    }
}
