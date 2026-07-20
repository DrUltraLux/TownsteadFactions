package com.drultralux.townsteadfactions.factions.voting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A single active leadership vote: its type, target, timing, and the
 * choices cast by players so far. Villager participation is
 * intentionally not stored here — since villager opinion can shift over
 * a vote's lifetime, their vote is computed fresh from current hearts
 * every time this vote is tallied, rather than cast once and locked in.
 */
public class VoteRecord {

    /** This vote's unique identifier. */
    private final UUID voteId;

    /** The faction this vote belongs to. */
    private final String factionId;

    /** The kind of vote this is, determining eligibility and threshold. */
    private final VoteType type;

    /**
     * The subject of this vote: the candidate being considered for
     * {@link VoteType#ELECT}/{@link VoteType#ELECT_MONARCH}, or the
     * leader being considered for removal under {@link VoteType#DEMOTE}.
     */
    private final UUID targetUUID;

    /** The player who nominated this vote, or {@code null} if not applicable (e.g. an automatic Monarch-elevation vote). */
    private final UUID nominatorUUID;

    /** The real-world time, in milliseconds since the epoch, this vote started. */
    private final long startTimestamp;

    /** The real-world time, in milliseconds since the epoch, this vote expires if not already resolved. */
    private final long expiryTimestamp;

    /** Each player's cast choice so far, keyed by UUID. Villager participation is computed separately, not stored here. */
    private final Map<UUID, VoteChoice> playerVotes = new HashMap<>();

    /**
     * Creates a new vote record.
     *
     * @param voteId this vote's unique identifier
     * @param factionId the faction this vote belongs to
     * @param type the kind of vote this is
     * @param targetUUID the vote's subject (candidate or leader-to-remove)
     * @param nominatorUUID the player who started this vote, or {@code null} if not applicable
     * @param startTimestamp the real-world time this vote started, in milliseconds since the epoch
     * @param expiryTimestamp the real-world time this vote expires, in milliseconds since the epoch
     */
    public VoteRecord(UUID voteId, String factionId, VoteType type, UUID targetUUID, UUID nominatorUUID, long startTimestamp, long expiryTimestamp) {
        this.voteId = voteId;
        this.factionId = factionId;
        this.type = type;
        this.targetUUID = targetUUID;
        this.nominatorUUID = nominatorUUID;
        this.startTimestamp = startTimestamp;
        this.expiryTimestamp = expiryTimestamp;
    }

    /**
     * Returns this vote's unique identifier.
     *
     * @return the vote ID
     */
    public UUID getVoteId() {
        return this.voteId;
    }

    /**
     * Returns the faction this vote belongs to.
     *
     * @return the faction ID
     */
    public String getFactionId() {
        return this.factionId;
    }

    /**
     * Returns the kind of vote this is.
     *
     * @return the vote type
     */
    public VoteType getType() {
        return this.type;
    }

    /**
     * Returns this vote's subject.
     *
     * @return the target UUID
     */
    public UUID getTargetUUID() {
        return this.targetUUID;
    }

    /**
     * Returns the player who nominated this vote.
     *
     * @return the nominator's UUID, or {@code null} if not applicable
     */
    public UUID getNominatorUUID() {
        return this.nominatorUUID;
    }

    /**
     * Returns when this vote started.
     *
     * @return the start time, in milliseconds since the epoch
     */
    public long getStartTimestamp() {
        return this.startTimestamp;
    }

    /**
     * Returns when this vote expires.
     *
     * @return the expiry time, in milliseconds since the epoch
     */
    public long getExpiryTimestamp() {
        return this.expiryTimestamp;
    }

    /**
     * Returns the live map of player choices cast so far. Modifying the
     * returned map modifies this vote directly — intended for use by
     * {@code VoteManager} and the save/load path only.
     *
     * @return the cast player votes, keyed by UUID
     */
    public Map<UUID, VoteChoice> getPlayerVotes() {
        return this.playerVotes;
    }

    /**
     * Records or updates a player's cast choice on this vote.
     *
     * @param playerUUID the voting player
     * @param choice the choice to record; {@code null} removes any existing vote (equivalent to un-voting)
     */
    public void castPlayerVote(UUID playerUUID, VoteChoice choice) {
        if (playerUUID == null) return;
        if (choice == null) {
            this.playerVotes.remove(playerUUID);
        } else {
            this.playerVotes.put(playerUUID, choice);
        }
    }

    /**
     * Checks whether this vote has passed its expiry time.
     *
     * @param currentTimeMillis the current real-world time, in milliseconds since the epoch
     * @return {@code true} if this vote has expired
     */
    public boolean isExpired(long currentTimeMillis) {
        return currentTimeMillis >= this.expiryTimestamp;
    }
}