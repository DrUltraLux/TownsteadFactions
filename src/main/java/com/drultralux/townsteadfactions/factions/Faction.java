package com.drultralux.townsteadfactions.factions;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a single faction: its identity, currency balances, unified
 * player/villager participant roster, and the set of origins allowed to
 * join it.
 */
public class Faction {

    /** This faction's unique, immutable identifier. */
    private final String id;

    /** This faction's human-readable display name. */
    private String displayName;

    /** Origin identifiers whose members are allowed to join this faction. */
    private final List<String> validOrigins = new ArrayList<>();

    /**
     * This faction's participants — players and villagers alike, unified
     * into one map keyed by UUID. See {@link FactionParticipant} for how
     * the two kinds differ.
     */
    private final Map<UUID, FactionParticipant> participants = new LinkedHashMap<>();

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
     */
    public Faction(String id, String displayName) {
        this.id = id != null ? id.trim() : "";
        this.displayName = displayName != null ? displayName.trim() : "";
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
     * Returns this faction's participants, keyed by UUID. Modifying the
     * returned map modifies this faction directly — intended for use by
     * {@code FactionManager} and the save/load path only.
     *
     * @return the participant roster
     */
    public Map<UUID, FactionParticipant> getParticipants() {
        return this.participants;
    }

    /**
     * Adds a player to this faction as a regular (non-leader) member, if
     * not already present, joining at the current time.
     *
     * @param playerUUID the UUID of the player to add
     */
    public void addPlayerParticipant(UUID playerUUID) {
        if (playerUUID != null) {
            this.participants.putIfAbsent(playerUUID, FactionParticipant.createPlayer(playerUUID, System.currentTimeMillis()));
        }
    }

    /**
     * Restores a player participant with a specific join time, trusting
     * it as-is. Intended for the save/load path only, so a reloaded
     * player's real join date is preserved rather than reset to "now".
     *
     * @param playerUUID the UUID of the player to restore
     * @param joinTimestamp their real join time, in milliseconds since the epoch
     * @param leader whether they were a leader when saved
     */
    public void restorePlayerParticipant(UUID playerUUID, long joinTimestamp, boolean leader) {
        if (playerUUID == null) return;
        FactionParticipant participant = FactionParticipant.createPlayer(playerUUID, joinTimestamp);
        participant.setLeader(leader);
        this.participants.put(playerUUID, participant);
    }

    /**
     * Adds a new villager participant, or updates an existing one's
     * cached display fields if already present. Called by the village
     * census sweep, which re-verifies every resident on each visit.
     *
     * @param villagerUUID the villager's UUID
     * @param name the villager's current display name
     * @param rootId the villager's current origin (root) ID
     * @param title the villager's current resolved display title
     */
    public void addOrUpdateVillagerParticipant(UUID villagerUUID, String name, String rootId, FactionTitle title) {
        if (villagerUUID == null) return;
        FactionParticipant existing = this.participants.get(villagerUUID);
        if (existing != null && existing.isVillager()) {
            existing.updateVillagerCache(name, rootId, title);
        } else {
            this.participants.put(villagerUUID, FactionParticipant.createVillager(villagerUUID, name, rootId, title));
        }
    }

    /**
     * Restores a villager participant exactly as saved, trusting the
     * given leader status. Intended for the save/load path only.
     *
     * @param villagerUUID the villager's UUID
     * @param name the villager's saved display name
     * @param rootId the villager's saved origin (root) ID
     * @param title the villager's saved resolved display title
     * @param leader whether they were a leader when saved
     */
    public void restoreVillagerParticipant(UUID villagerUUID, String name, String rootId, FactionTitle title, boolean leader) {
        if (villagerUUID == null) return;
        FactionParticipant participant = FactionParticipant.createVillager(villagerUUID, name, rootId, title);
        participant.setLeader(leader);
        this.participants.put(villagerUUID, participant);
    }

    /**
     * Removes a participant — player or villager — from this faction, if
     * present.
     *
     * @param uuid the UUID of the participant to remove
     */
    public void removeParticipant(UUID uuid) {
        if (uuid != null) {
            this.participants.remove(uuid);
        }
    }

    /**
     * Checks whether a participant currently holds a Leader role.
     *
     * @param uuid the participant to check
     * @return {@code true} if they're a current member/villager with the Leader role
     */
    public boolean isLeader(UUID uuid) {
        FactionParticipant participant = this.participants.get(uuid);
        return participant != null && participant.isLeader();
    }

    /**
     * Sets whether a participant currently holds a Leader role. Does
     * nothing if the given UUID isn't a current participant.
     *
     * @param uuid the participant to update
     * @param leader the new leader status
     */
    public void setLeader(UUID uuid, boolean leader) {
        FactionParticipant participant = this.participants.get(uuid);
        if (participant != null) {
            participant.setLeader(leader);
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
     * the oldest entries first, without adding a new one.
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