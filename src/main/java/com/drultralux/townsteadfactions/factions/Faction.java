package com.drultralux.townsteadfactions.factions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a single faction: its identity, leader, currency balances,
 * member list, and the set of origins allowed to join it.
 */
public class Faction {

    /** This faction's unique, immutable identifier. */
    private final String id;

    /** This faction's human-readable display name. */
    private String displayName;

    /** The UUID of this faction's leader, if one is set. */
    private UUID leaderUUID;

    /** Origin identifiers whose members are allowed to join this faction. */
    private final List<String> validOrigins = new ArrayList<>();

    /** UUIDs of this faction's current members. */
    private final List<UUID> members = new ArrayList<>();

    /** This faction's current cogs (currency) balance. */
    private int cogs = 0;

    /** This faction's current food balance. */
    private int food = 0;

    /** This faction's current mana balance. */
    private int mana = 0;

    /**
     * Creates a new faction.
     *
     * @param id the unique identifier for this faction (e.g. {@code "Mages"})
     * @param displayName the human-readable name shown in the UI
     * @param leaderUUID the UUID of this faction's leader, or {@code null} if none
     */
    public Faction(String id, String displayName, UUID leaderUUID) {
        this.id = id != null ? id.trim() : "";
        this.displayName = displayName != null ? displayName.trim() : "";
        this.leaderUUID = leaderUUID;
    }

    /**
     * Returns this faction's unique identifier.
     *
     * @return the faction ID
     */
    public String getId() {
        return this.id;
    }

    /**
     * Returns this faction's display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return this.displayName;
    }

    /**
     * Sets this faction's display name.
     *
     * @param displayName the new display name; ignored if {@code null}
     */
    public void setDisplayName(String displayName) {
        if (displayName != null) {
            this.displayName = displayName.trim();
        }
    }

    /**
     * Returns the UUID of this faction's leader.
     *
     * @return the leader's UUID, or {@code null} if none is set
     */
    public UUID getLeaderUUID() {
        return this.leaderUUID;
    }

    /**
     * Sets this faction's leader.
     *
     * @param leaderUUID the new leader's UUID, or {@code null} to clear it
     */
    public void setLeaderUUID(UUID leaderUUID) {
        this.leaderUUID = leaderUUID;
    }

    /**
     * Returns the list of origin identifiers allowed to join this faction.
     *
     * @return the valid origins for this faction
     */
    public List<String> getValidOrigins() {
        return this.validOrigins;
    }

    /**
     * Replaces this faction's list of valid origins. Blank or {@code null}
     * entries are skipped.
     *
     * @param origins the new list of valid origin identifiers, or {@code null} to clear
     */
    public void setValidOrigins(List<String> origins) {
        this.validOrigins.clear();
        if (origins != null) {
            for (String origin : origins) {
                if (origin != null && !origin.trim().isEmpty()) {
                    this.validOrigins.add(origin.trim());
                }
            }
        }
    }

    /**
     * Returns this faction's current members.
     *
     * @return the UUIDs of this faction's members
     */
    public List<UUID> getMembers() {
        return this.members;
    }

    /**
     * Adds a player to this faction, if not already a member.
     *
     * @param playerUUID the UUID of the player to add
     */
    public void addMember(UUID playerUUID) {
        if (playerUUID != null && !this.members.contains(playerUUID)) {
            this.members.add(playerUUID);
        }
    }

    /**
     * Removes a player from this faction, if they are a member.
     *
     * @param playerUUID the UUID of the player to remove
     */
    public void removeMember(UUID playerUUID) {
        if (playerUUID != null) {
            this.members.remove(playerUUID);
        }
    }

    /**
     * Returns this faction's current cogs balance.
     *
     * @return the cogs balance
     */
    public int getCogs() { return this.cogs; }

    /**
     * Sets this faction's cogs balance.
     *
     * @param value the new cogs balance
     */
    public void setCogs(int value) { this.cogs = value; }

    /**
     * Returns this faction's current food balance.
     *
     * @return the food balance
     */
    public int getFood() { return this.food; }

    /**
     * Sets this faction's food balance.
     *
     * @param value the new food balance
     */
    public void setFood(int value) { this.food = value; }

    /**
     * Returns this faction's current mana balance.
     *
     * @return the mana balance
     */
    public int getMana() { return this.mana; }

    /**
     * Sets this faction's mana balance.
     *
     * @param value the new mana balance
     */
    public void setMana(int value) { this.mana = value; }
}