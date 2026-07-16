package com.drultralux.townsteadfactions.factions;

import com.drultralux.townsteadfactions.LogManager;
import com.drultralux.townsteadfactions.TownsteadFactions;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.Map;
import java.util.UUID;

/**
 * Handles server-side disk serialization and world data persistence for the Factions engine.
 * Implements the standard NeoForge 1.21.1 SavedData system to store state compounds.
 */
public class FactionSavedData extends SavedData {
    private static final String DATA_NAME = TownsteadFactions.MODID + "_world_data";

    /**
     * Required modern factory definition setup configuration for NeoForge world storage engine lookups.
     */
    public static final SavedData.Factory<FactionSavedData> FACTORY = new SavedData.Factory<>(
            FactionSavedData::new,
            FactionSavedData::load,
            net.minecraft.util.datafix.DataFixTypes.LEVEL
    );

    /**
     * Internal blank constructor utilized dynamically by the storage system framework factories.
     */
    public FactionSavedData() {
        LogManager.debug("Fresh world data persistence matrix instantiated.");
    }

    /**
     * Acquires or allocates the definitive global FactionSavedData registry instance from the server Overworld storage.
     *
     * @param server the running MinecraftServer engine context instance
     * @return the active, bound server-side FactionSavedData persistence proxy worker
     */
    public static FactionSavedData get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(FACTORY, DATA_NAME);
    }

    /**
     * Reads a world save CompoundTag from disk and uses it to fully re-inflate the active operational FactionManager maps.
     */
    public static FactionSavedData load(CompoundTag nbt, HolderLookup.Provider registries) {
        LogManager.info("Reading faction state matrices out of disk storage...");
        FactionSavedData data = new FactionSavedData();
        FactionManager manager = FactionManager.getInstance();

        // 1. First ensure the system runtime maps have been populated with baseline config models
        manager.initializeFactionsFromConfig();

        // 2. Load the structural player-to-faction global routing maps index
        if (nbt.contains("GlobalPlayerMap", Tag.TAG_COMPOUND)) {
            CompoundTag playerMapTag = nbt.getCompound("GlobalPlayerMap");
            for (String key : playerMapTag.getAllKeys()) {
                try {
                    UUID playerUUID = UUID.fromString(key);
                    String factionId = playerMapTag.getString(key);
                    manager.assignPlayerToFaction(playerUUID, factionId);
                } catch (Exception e) {
                    LogManager.warn("Skipping corrupt player index entry token block during deserialization: " + key);
                }
            }
        }

        // 3. De-serialize and inject custom stored values straight back into the true Faction objects
        if (nbt.contains("FactionsDataMatrix", Tag.TAG_LIST)) {
            ListTag factionsList = nbt.getList("FactionsDataMatrix", Tag.TAG_COMPOUND);
            for (int i = 0; i < factionsList.size(); i++) {
                CompoundTag factionTag = factionsList.getCompound(i);
                String factionId = factionTag.getString("FactionID");

                Faction faction = manager.getFaction(factionId);
                if (faction != null) {
                    // Update dynamic balance values
                    faction.setCogs(factionTag.getInt("Cogs"));
                    faction.setFood(factionTag.getInt("Food"));
                    faction.setMana(factionTag.getInt("Mana"));
                    if (factionTag.hasUUID("LeaderUUID")) {
                        faction.setLeaderUUID(factionTag.getUUID("LeaderUUID"));
                    }

                    // Reconstruct internal nested social hierarchies and rosters natively
                    if (factionTag.contains("SocialRoster", Tag.TAG_LIST)) {
                        ListTag rosterList = factionTag.getList("SocialRoster", Tag.TAG_COMPOUND);
                        for (int j = 0; j < rosterList.size(); j++) {
                            CompoundTag profileTag = rosterList.getCompound(j);
                            try {
                                UUID memberUUID = profileTag.getUUID("MemberUUID");
                                FactionTitle title = FactionTitle.valueOf(profileTag.getString("TitleRole"));
                                faction.addOrUpdateMember(memberUUID, title);
                            } catch (Exception e) {
                                LogManager.warn("[" + factionId + "] Failed to re-inflate member profile row block element.");
                            }
                        }
                    }
                }
            }
        }

        LogManager.info("World state matrix re-inflation cycle processed successfully.");
        return data;
    }

    /**
     * Compiles data variables directly out of live object configurations and flushes them down to a structural file compound.
     */
    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        LogManager.debug("Serializing active faction data instances down to standard NBT structures...");
        FactionManager manager = FactionManager.getInstance();

        // 1. Serialize the player index routing allocations mapping
        CompoundTag playerMapTag = new CompoundTag();
        for (Map.Entry<String, Faction> factionEntry : manager.getActiveFactions().entrySet()) {
            for (UUID playerUuid : factionEntry.getValue().getMemberRoster().keySet()) {
                playerMapTag.putString(playerUuid.toString(), factionEntry.getKey());
            }
        }
        nbt.put("GlobalPlayerMap", playerMapTag);

        // 2. Serialize all customized operational indicators inside the actual faction tracking configurations
        ListTag factionsList = new ListTag();
        for (Map.Entry<String, Faction> entry : manager.getActiveFactions().entrySet()) {
            Faction faction = entry.getValue();
            CompoundTag factionTag = new CompoundTag();

            factionTag.putString("FactionID", faction.getFactionID());
            factionTag.putUUID("LeaderUUID", faction.getLeaderUUID());
            factionTag.putInt("Cogs", faction.getCogs());
            factionTag.putInt("Food", faction.getFood());
            factionTag.putInt("Mana", faction.getMana());

            // Deep nested serialization loop tracking independent social profile records
            ListTag rosterList = new ListTag();
            for (MemberProfile profile : faction.getMemberRoster().values()) {
                CompoundTag profileTag = new CompoundTag();
                profileTag.putUUID("MemberUUID", profile.getPlayerUUID());
                profileTag.putString("TitleRole", profile.getTitle().name());
                rosterList.add(profileTag);
            }
            factionTag.put("SocialRoster", rosterList);
            factionsList.add(factionTag);
        }
        nbt.put("FactionsDataMatrix", factionsList);

        return nbt;
    }

    /**
     * Standardized hook utility method to mark world structures dirty, scheduling a background disk flush action.
     */
    public void forceSaveFlush() {
        this.setDirty();
        LogManager.debug("World persistence state marked dirty. Preparing synchronous background IO data stream flush.");
    }
}