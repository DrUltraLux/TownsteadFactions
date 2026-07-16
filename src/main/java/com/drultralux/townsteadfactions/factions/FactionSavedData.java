package com.drultralux.townsteadfactions.factions;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Centrally manages the binary NBT persistence layer serialization for live faction profiles.
 * Flushes active currency balances and membership registers directly down to world files on save checkpoints.
 */
public class FactionSavedData extends SavedData {

    public static FactionSavedData load(CompoundTag nbt, HolderLookup.Provider registries) {
        FactionSavedData data = new FactionSavedData();
        if (nbt.contains("FactionsDataMatrix", Tag.TAG_LIST)) {
            ListTag factionsList = nbt.getList("FactionsDataMatrix", Tag.TAG_COMPOUND);

            for (int i = 0; i < factionsList.size(); i++) {
                CompoundTag factionTag = factionsList.getCompound(i);
                String factionId = factionTag.getString("factionID");

                Faction faction = FactionManager.getInstance().getActiveFactions().get(factionId);
                if (faction != null) {
                    faction.setCogs(factionTag.getInt("Cogs"));
                    faction.setFood(factionTag.getInt("Food"));
                    faction.setMana(factionTag.getInt("Mana"));

                    if (factionTag.hasUUID("LeaderUUID")) {
                        faction.setLeaderUUID(factionTag.getUUID("LeaderUUID"));
                    }

                    if (factionTag.contains("MembersRoster", Tag.TAG_LIST)) {
                        ListTag membersList = factionTag.getList("MembersRoster", Tag.TAG_COMPOUND);
                        for (int j = 0; j < membersList.size(); j++) {
                            CompoundTag memberTag = membersList.getCompound(j);
                            if (memberTag.hasUUID("MemberUUID")) {
                                faction.addMember(memberTag.getUUID("MemberUUID"));
                            }
                        }
                    }
                }
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        ListTag factionsList = new ListTag();
        Map<String, Faction> activeMap = FactionManager.getInstance().getActiveFactions();

        for (Map.Entry<String, Faction> entry : activeMap.entrySet()) {
            CompoundTag factionTag = new CompoundTag();
            Faction faction = entry.getValue();

            factionTag.putString("factionID", faction.getId());
            factionTag.putInt("Cogs", faction.getCogs());
            factionTag.putInt("Food", faction.getFood());
            factionTag.putInt("Mana", faction.getMana());

            if (faction.getLeaderUUID() != null) {
                factionTag.putUUID("LeaderUUID", faction.getLeaderUUID());
            }

            ListTag membersList = new ListTag();
            for (UUID memberUUID : faction.getMembers()) {
                CompoundTag memberTag = new CompoundTag();
                memberTag.putUUID("MemberUUID", memberUUID);
                membersList.add(memberTag);
            }
            factionTag.put("MembersRoster", membersList);

            factionsList.add(factionTag);
        }

        nbt.put("FactionsDataMatrix", factionsList);
        return nbt;
    }
}