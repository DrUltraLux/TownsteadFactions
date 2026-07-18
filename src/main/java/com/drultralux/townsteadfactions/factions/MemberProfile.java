package com.drultralux.townsteadfactions.factions;

import com.drultralux.townsteadfactions.utils.LogManager;
import java.util.UUID;

/**
 * Represents a single member's status within a faction: their identity,
 * assigned title, and when they joined.
 */
public class MemberProfile {

    /** The UUID of the player this profile belongs to. */
    private final UUID playerUUID;

    /** The member's current title within the faction. */
    private FactionTitle title;

    /** The time this profile was created, in milliseconds since the epoch. */
    private final long joinTimestamp;

    /**
     * Creates a new member profile for a player joining a faction.
     *
     * @param playerUUID the UUID of the joining player
     * @param title the initial title to assign
     */
    public MemberProfile(UUID playerUUID, FactionTitle title) {
        this.playerUUID = playerUUID;
        this.title = title;
        this.joinTimestamp = System.currentTimeMillis();
        LogManager.debug("Member profile instantiated for UUID: " + playerUUID + " with role: " + title.name());
    }

    /**
     * Returns the UUID of the player this profile belongs to.
     *
     * @return the player's UUID
     */
    public UUID getPlayerUUID() {
        return this.playerUUID;
    }

    /**
     * Returns the member's current title.
     *
     * @return the current {@link FactionTitle}
     */
    public FactionTitle getTitle() {
        return this.title;
    }

    /**
     * Updates the member's title.
     *
     * @param title the new title to assign
     */
    public void setTitle(FactionTitle title) {
        LogManager.debug("Updating profile status for " + this.playerUUID + " from " + this.title.name() + " to " + title.name());
        this.title = title;
    }

    /**
     * Returns when this profile was created.
     *
     * @return the join time, in milliseconds since the epoch
     */
    public long getJoinTimestamp() {
        return this.joinTimestamp;
    }
}