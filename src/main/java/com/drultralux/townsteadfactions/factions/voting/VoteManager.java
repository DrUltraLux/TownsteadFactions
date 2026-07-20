package com.drultralux.townsteadfactions.factions.voting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The sole gateway for active leadership vote storage: creating,
 * retrieving, listing, and removing {@link VoteRecord}s. Mirrors
 * {@code FactionManager}'s ownership pattern — other classes only touch
 * vote data through this class's methods.
 *
 * <p>This class only manages storage. It knows nothing about voting
 * eligibility, thresholds, tallying, or what happens when a vote
 * resolves — that's the responsibility of the leadership orchestration
 * layer built in a later stage.</p>
 */
public final class VoteManager {

    /** The currently bound world save data instance, or {@code null} if none is bound. */
    private static VoteSavedData activeStorageInstance = null;

    private VoteManager() {}

    /**
     * Binds the world save data instance backing this manager's state.
     *
     * @param storage the world save data instance to bind
     */
    public static void setStorageInstance(VoteSavedData storage) {
        activeStorageInstance = storage;
    }

    /**
     * Creates and stores a new active vote.
     *
     * @param factionId the faction this vote belongs to
     * @param type the kind of vote this is
     * @param targetUUID the vote's subject (candidate or leader-to-remove)
     * @param nominatorUUID the player who started this vote, or {@code null} if not applicable
     * @param durationMillis how long this vote should remain open before expiring, in milliseconds
     * @return the newly created vote record
     */
    public static VoteRecord startVote(String factionId, VoteType type, UUID targetUUID, UUID nominatorUUID, long durationMillis) {
        UUID voteId = UUID.randomUUID();
        long now = System.currentTimeMillis();
        VoteRecord record = new VoteRecord(voteId, factionId, type, targetUUID, nominatorUUID, now, now + durationMillis);

        if (activeStorageInstance != null) {
            activeStorageInstance.activeVotes.put(voteId, record);
            activeStorageInstance.setDirty();
        }
        return record;
    }

    /**
     * Returns a specific active vote by ID.
     *
     * @param voteId the vote to look up
     * @return the matching vote record, or {@code null} if it doesn't exist or isn't active
     */
    public static VoteRecord getVote(UUID voteId) {
        if (activeStorageInstance == null || voteId == null) return null;
        return activeStorageInstance.activeVotes.get(voteId);
    }

    /**
     * Returns every currently active vote for a faction.
     *
     * @param factionId the faction to look up votes for
     * @return the faction's active votes; empty if none
     */
    public static List<VoteRecord> getActiveVotesForFaction(String factionId) {
        List<VoteRecord> result = new ArrayList<>();
        if (activeStorageInstance == null || factionId == null) return result;
        for (VoteRecord record : activeStorageInstance.activeVotes.values()) {
            if (factionId.equals(record.getFactionId())) {
                result.add(record);
            }
        }
        return result;
    }

    /**
     * Returns every currently active vote, across all factions. Used by
     * the periodic resolution ticker (built in a later stage) to sweep
     * every vote for expiry or a newly-crossed threshold.
     *
     * @return every active vote
     */
    public static List<VoteRecord> getAllActiveVotes() {
        if (activeStorageInstance == null) return new ArrayList<>();
        return new ArrayList<>(activeStorageInstance.activeVotes.values());
    }

    /**
     * Records or updates a player's cast choice on an active vote, and
     * flags the save data as dirty.
     *
     * @param voteId the vote to cast on
     * @param playerUUID the voting player
     * @param choice the choice to record; {@code null} removes any existing vote
     */
    public static void castVote(UUID voteId, UUID playerUUID, VoteChoice choice) {
        VoteRecord record = getVote(voteId);
        if (record == null) return;
        record.castPlayerVote(playerUUID, choice);
        if (activeStorageInstance != null) {
            activeStorageInstance.setDirty();
        }
    }

    /**
     * Removes a vote from active storage — used once a vote has been
     * resolved (passed, failed, or expired) and its outcome applied, so
     * resolved votes are never persisted or tallied again.
     *
     * @param voteId the vote to remove
     */
    public static void removeVote(UUID voteId) {
        if (activeStorageInstance == null || voteId == null) return;
        if (activeStorageInstance.activeVotes.remove(voteId) != null) {
            activeStorageInstance.setDirty();
        }
    }
}