package com.drultralux.townsteadfactions.factions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Data object container representing a singular live faction instance.
 * Manages currency balances, tracked member UUID profiles, and valid blueprint configurations.
 */
public class Faction {
    private final String id;
    private String displayName;
    private UUID leaderUUID;

    // Tracked allowed structural components
    private final List<String> validOrigins = new ArrayList<>();
    private final List<UUID> members = new ArrayList<>();

    // Faction Currency Metrics called directly by FactionCommands
    private int cogs = 0;
    private int food = 0;
    private int mana = 0;

    /**
     * Constructs a pristine Faction data tracking instance.
     *
     * @param id          The clean unique identifier key (e.g., "Mages")
     * @param displayName The human-readable interface title string
     * @param leaderUUID  The UUID of the managing group leader or system system anchor
     */
    public Faction(String id, String displayName, UUID leaderUUID) {
        this.id = id != null ? id.trim() : "";
        this.displayName = displayName != null ? displayName.trim() : "";
        this.leaderUUID = leaderUUID;
    }

    public String getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(String displayName) {
        if (displayName != null) {
            this.displayName = displayName.trim();
        }
    }

    public UUID getLeaderUUID() {
        return this.leaderUUID;
    }

    public void setLeaderUUID(UUID leaderUUID) {
        this.leaderUUID = leaderUUID;
    }

    public List<String> getValidOrigins() {
        return this.validOrigins;
    }

    /**
     * Updates the validated list of allowed Townstead origin strings for this faction structure.
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

    public List<UUID> getMembers() {
        return this.members;
    }

    /**
     * Attaches a member directly to this faction registry instance.
     */
    public void addMember(UUID playerUUID) {
        if (playerUUID != null && !this.members.contains(playerUUID)) {
            this.members.add(playerUUID);
        }
    }

    /**
     * Safely removes a member from this faction profile tracking sheet.
     */
    public void removeMember(UUID playerUUID) {
        if (playerUUID != null) {
            this.members.remove(playerUUID);
        }
    }

    public int getCogs() { return this.cogs; }
    public void setCogs(int value) { this.cogs = value; }

    public int getFood() { return this.food; }
    public void setFood(int value) { this.food = value; }

    public int getMana() { return this.mana; }
    public void setMana(int value) { this.mana = value; }
}
