package com.drultralux.townsteadfactions.factions.voting;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.saveddata.SavedData;
import com.drultralux.townsteadfactions.utils.LogManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persists all currently active leadership votes to the world save.
 * Resolved votes are never persisted — once a vote passes, fails, or
 * expires, its outcome is applied and logged immediately, and the record
 * itself is discarded rather than kept around.
 */
public class VoteSavedData extends SavedData {

    /** All currently active votes, keyed by vote ID. */
    public final Map<UUID, VoteRecord> activeVotes = new HashMap<>();

    /**
     * Loads active vote records from an NBT tag.
     *
     * @param tag the NBT tag loaded from disk, or {@code null} if none exists
     * @param registries the registry lookup provider, unused here but required by the {@link SavedData} API
     * @return a new {@code VoteSavedData} populated from the tag
     */
    public static VoteSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        VoteSavedData data = new VoteSavedData();
        if (tag != null && tag.contains("activeVotes", 9)) { // 9 is ListTag
            ListTag votesList = tag.getList("activeVotes", 10); // 10 is CompoundTag
            for (int i = 0; i < votesList.size(); i++) {
                CompoundTag voteTag = votesList.getCompound(i);
                try {
                    UUID voteId = voteTag.getUUID("voteId");
                    String factionId = voteTag.getString("factionId");
                    VoteType type = VoteType.valueOf(voteTag.getString("type"));
                    UUID targetUUID = voteTag.getUUID("targetUUID");
                    UUID nominatorUUID = voteTag.hasUUID("nominatorUUID") ? voteTag.getUUID("nominatorUUID") : null;
                    long startTimestamp = voteTag.getLong("startTimestamp");
                    long expiryTimestamp = voteTag.getLong("expiryTimestamp");

                    VoteRecord record = new VoteRecord(voteId, factionId, type, targetUUID, nominatorUUID, startTimestamp, expiryTimestamp);

                    if (voteTag.contains("playerVotes", 10)) { // 10 is CompoundTag
                        CompoundTag playerVotesTag = voteTag.getCompound("playerVotes");
                        for (String key : playerVotesTag.getAllKeys()) {
                            try {
                                UUID playerUUID = UUID.fromString(key);
                                VoteChoice choice = VoteChoice.valueOf(playerVotesTag.getString(key));
                                record.castPlayerVote(playerUUID, choice);
                            } catch (Exception e) {
                                LogManager.warn("Skipping malformed cast vote entry for player '" + key + "' in vote " + voteId + ".");
                            }
                        }
                    }

                    data.activeVotes.put(voteId, record);
                } catch (Exception e) {
                    LogManager.warn("Skipping malformed saved vote entry at index " + i + ".");
                }
            }
        }
        return data;
    }

    /**
     * Serializes all active vote records into the given NBT tag.
     *
     * @param tag the NBT tag to write into
     * @param provider the registry lookup provider, unused here but required by the {@link SavedData} API
     * @return the same {@code tag}, populated
     */
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag votesList = new ListTag();
        for (VoteRecord record : activeVotes.values()) {
            CompoundTag voteTag = new CompoundTag();
            voteTag.putUUID("voteId", record.getVoteId());
            voteTag.putString("factionId", record.getFactionId());
            voteTag.putString("type", record.getType().name());
            voteTag.putUUID("targetUUID", record.getTargetUUID());
            if (record.getNominatorUUID() != null) {
                voteTag.putUUID("nominatorUUID", record.getNominatorUUID());
            }
            voteTag.putLong("startTimestamp", record.getStartTimestamp());
            voteTag.putLong("expiryTimestamp", record.getExpiryTimestamp());

            CompoundTag playerVotesTag = new CompoundTag();
            record.getPlayerVotes().forEach((uuid, choice) -> playerVotesTag.putString(uuid.toString(), choice.name()));
            voteTag.put("playerVotes", playerVotesTag);

            votesList.add(voteTag);
        }
        tag.put("activeVotes", votesList);
        return tag;
    }
}
