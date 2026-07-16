package com.drultralux.townsteadfactions.factions;

import com.drultralux.townsteadfactions.LogManager;
import java.util.UUID;

/**
 * Encapsulates individual membership information and hierarchy statuses within a parent Faction.
 * Eliminates unlinked floating maps by storing entity records directly in an object schema.
 */
public class MemberProfile {
    private final UUID playerUUID;
    private FactionTitle title;
    private final long joinTimestamp;

    /**
     * Constructs a fresh membership record profile for a newly assigned player.
     *
     * @param playerUUID the unique identifier string tracking the specific user entity
     * @param title the initial hierarchy role level assigned to the member profile
     */
    public MemberProfile(UUID playerUUID, FactionTitle title) {
        this.playerUUID = playerUUID;
        this.title = title;
        this.joinTimestamp = System.currentTimeMillis();
        LogManager.debug("Member profile instantiated for UUID: " + playerUUID + " with role: " + title.name());
    }

    /**
     * Gets the unique identifier tracking token for this profile.
     *
     * @return the unique UUID tracking token assigned to the player entity
     */
    public UUID getPlayerUUID() {
        return this.playerUUID;
    }

    /**
     * Gets the current hierarchy tracking title held by this user.
     *
     * @return the active FactionTitle configuration enum token
     */
    public FactionTitle getTitle() {
        return this.title;
    }

    /**
     * Updates the active authority tier level held by the player profile.
     *
     * @param title the newly assigned role ranking structure token
     */
    public void setTitle(FactionTitle title) {
        LogManager.debug("Updating profile status for " + this.playerUUID + " from " + this.title.name() + " to " + title.name());
        this.title = title;
    }

    /**
     * Gets the system epoch timestamp tracking exactly when this profile was introduced.
     *
     * @return the long representation of the system millisecond record
     */
    public long getJoinTimestamp() {
        return this.joinTimestamp;
    }
}
