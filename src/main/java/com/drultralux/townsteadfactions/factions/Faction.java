package com.drultralux.townsteadfactions.factions;

import com.drultralux.townsteadfactions.LogManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Acts as the absolute structural authority representing a single faction domain instance.
 * Governs localized balances, social hierarchy data rosters, and derived account attributes internally.
 */
public class Faction {
    private final String factionID;
    private final String displayName;
    private UUID leaderUUID;

    // Localized base currency asset indicators
    private int cogs;
    private int food;
    private int mana;

    // Self-contained social grid tracking collections
    private final Map<UUID, MemberProfile> memberRoster = new HashMap<>();

    /**
     * Instantiates an official object tracking context for a structural faction unit.
     *
     * @param factionID the clean programmatic tracking name configuration identifier string
     * @param displayName the localized text label representation used for front-end rendering displays
     * @param leaderUUID the unique identifier string representing the absolute leadership authority
     */
    public Faction(String factionID, String displayName, UUID leaderUUID) {
        this.factionID = factionID;
        this.displayName = displayName;
        this.leaderUUID = leaderUUID;

        // Automatically append the absolute administrator directly to the roster profiles mapping
        this.memberRoster.put(leaderUUID, new MemberProfile(leaderUUID, FactionTitle.LEADER));
        LogManager.debug("True Faction domain model initialized. ID: " + factionID + " | Leader: " + leaderUUID);
    }

    public String getFactionID() {
        return this.factionID;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public UUID getLeaderUUID() {
        return this.leaderUUID;
    }

    public void setLeaderUUID(UUID leaderUUID) {
        LogManager.debug("[" + this.factionID + "] Migrating leadership tracking anchor to: " + leaderUUID);
        this.leaderUUID = leaderUUID;
        addOrUpdateMember(leaderUUID, FactionTitle.LEADER);
    }

    public int getCogs() { return this.cogs; }
    public void setCogs(int cogs) { this.cogs = cogs; }

    public int getFood() { return this.food; }
    public void setFood(int food) { this.food = food; }

    public int getMana() { return this.mana; }
    public void setMana(int mana) { this.mana = mana; }

    /**
     * Safely updates or appends a structural player identity record to the self-contained collection.
     *
     * @param playerUUID the target tracking key profile identifier
     * @param title the structural hierarchy tier rank to assign
     */
    public void addOrUpdateMember(UUID playerUUID, FactionTitle title) {
        if (this.memberRoster.containsKey(playerUUID)) {
            this.memberRoster.get(playerUUID).setTitle(title);
        } else {
            this.memberRoster.put(playerUUID, new MemberProfile(playerUUID, title));
            LogManager.debug("[" + this.factionID + "] Member appended to roster: " + playerUUID + " as " + title.name());
        }
    }

    /**
     * Removes an active member registration trace profile out of the tracking roster collections.
     *
     * @param playerUUID the target unique identity key token to isolate and drop
     */
    public void removeMember(UUID playerUUID) {
        if (playerUUID.equals(this.leaderUUID)) {
            LogManager.warn("[" + this.factionID + "] Refusing profile deletion tracking sequence for the root Leader entity.");
            return;
        }
        if (this.memberRoster.remove(playerUUID) != null) {
            LogManager.debug("[" + this.factionID + "] Removed member profile lookup reference for: " + playerUUID);
        }
    }

    /**
     * Resolves the stored local title level assigned to an identity key inside this faction context.
     *
     * @param playerUUID the unique target user identity token to process
     * @return the localized fallback FactionTitle designation token
     */
    public FactionTitle getPlayerLocalTitle(UUID playerUUID) {
        if (playerUUID.equals(this.leaderUUID)) {
            return FactionTitle.LEADER;
        }
        MemberProfile profile = this.memberRoster.get(playerUUID);
        return profile != null ? profile.getTitle() : FactionTitle.MEMBER;
    }

    /**
     * Exposes the internal immutable tracking collection for external queries or iteration loops.
     *
     * @return a mapped reference view containing all encapsulated member profiles
     */
    public Map<UUID, MemberProfile> getMemberRoster() {
        return java.util.Collections.unmodifiableMap(this.memberRoster);
    }

    /**
     * Generates a consistent, unique synthetic bank account key derived from the faction ID.
     *
     * @return a predictable UUID string representation matching the faction target key namespace
     */
    public UUID getTreasuryAccountUUID() {
        if (this.factionID == null || this.factionID.isEmpty()) {
            return UUID.fromString("00000000-0000-0000-0000-000000000000");
        }
        return UUID.nameUUIDFromBytes(("townstead_faction:" + this.factionID).getBytes());
    }
}