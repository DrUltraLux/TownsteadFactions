package com.drultralux.townsteadfactions.factions;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a single faction: its identity, leader, currency balances,
 * member roster, and the set of origins allowed to join it.
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

    /**
     * This faction's members, keyed by UUID, in join order. Each member's
     * faction-internal role (Leader/Member) and join time live on their
     * {@link MemberProfile} — this is distinct from a player's
     * self-assigned cosmetic title or their Capitals-derived nobility
     * rank, both handled elsewhere.
     */
    private final Map<UUID, MemberProfile> members = new LinkedHashMap<>();

    /**
     * This faction's full activity history, oldest first. An
     * {@link ArrayDeque} rather than a list, since entries are only ever
     * appended at the end and trimmed from the front when over the
     * configured cap — both O(1) here, versus O(n) per removal with an
     * {@code ArrayList}.
     */
    private final Deque<ActivityLogEntry> activityLog = new ArrayDeque<>();

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
     * Returns this faction's members, keyed by UUID. Modifying the
     * returned map modifies this faction directly — intended for use by
     * {@code FactionManager} and the save/load path only.
     *
     * @return the member roster
     */
    public Map<UUID, MemberProfile> getMembers() {
        return this.members;
    }

    /**
     * Adds a player to this faction as a regular member, if not already
     * present.
     *
     * @param playerUUID the UUID of the player to add
     */
    public void addMember(UUID playerUUID) {
        if (playerUUID != null) {
            this.members.putIfAbsent(playerUUID, new MemberProfile(playerUUID, FactionTitle.MEMBER));
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
     * Restores a member profile as-is, without the "if not already
     * present" check {@link #addMember} applies. Intended for the save/load
     * path only, where the saved title and join time should be trusted
     * and reapplied directly.
     *
     * @param profile the member profile to restore
     */
    public void restoreMember(MemberProfile profile) {
        if (profile != null && profile.getPlayerUUID() != null) {
            this.members.put(profile.getPlayerUUID(), profile);
        }
    }

    /**
     * Checks whether a player currently holds the Leader role within this
     * faction's internal structure.
     *
     * @param playerUUID the player to check
     * @return {@code true} if they're a member with the Leader title
     */
    public boolean isLeader(UUID playerUUID) {
        MemberProfile profile = this.members.get(playerUUID);
        return profile != null && profile.getTitle() == FactionTitle.LEADER;
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

    /**
     * Appends an entry to this faction's activity log, trimming the
     * oldest entries if the given cap is exceeded.
     *
     * @param message the entry to record
     * @param cap the maximum number of entries to retain
     */
    public void addLogEntry(String message, int cap) {
        this.activityLog.addLast(new ActivityLogEntry(System.currentTimeMillis(), message));
        trimLogToCap(cap);
    }

    /**
     * Trims this faction's activity log down to the given cap, discarding
     * the oldest entries first, without adding a new one. Used both after
     * appending a new entry, and to immediately re-apply a lowered cap
     * after a config change, rather than waiting for the faction's next
     * activity to catch up.
     *
     * @param cap the maximum number of entries to retain
     */
    public void trimLogToCap(int cap) {
        while (this.activityLog.size() > cap) {
            this.activityLog.removeFirst();
        }
    }

    /**
     * Returns this faction's full activity log, oldest first. Modifying
     * the returned deque modifies this faction directly — intended for
     * use by {@code FactionManager} and the save/load path only.
     *
     * @return the activity log
     */
    public Deque<ActivityLogEntry> getActivityLog() {
        return this.activityLog;
    }
}