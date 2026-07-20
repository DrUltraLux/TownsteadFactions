package com.drultralux.townsteadfactions.factions;

import java.util.UUID;

/**
 * A single participant in a faction — player or villager, unified into
 * one type. Which fields are meaningful depends on {@link #isPlayer()}:
 * a villager's cached display fields always return {@code null} for a
 * player (player display data is resolved live elsewhere — the online
 * player list, {@code OriginManager}, {@code TitleManager} — rather than
 * cached, since a player's real state can change at any moment while
 * they're connected, unlike a villager who's only checked periodically
 * by the census sweep).
 *
 * <p>Instances are only ever created and mutated through {@code Faction}
 * — matching the rule that nothing outside {@code FactionManager} should
 * hold or construct a {@code Faction}'s internal data directly.</p>
 */
public class FactionParticipant {

    /** This participant's UUID — a player's account UUID, or a villager entity's UUID. */
    private final UUID uuid;

    /** Whether this participant is a player. If {@code false}, they're a villager. */
    private final boolean player;

    /**
     * Whether this participant currently holds a Leader role in the
     * faction. Works identically for players and villagers — there is no
     * separate leadership concept per participant kind.
     */
    private boolean leader = false;

    /** When this participant joined, in milliseconds since the epoch. Only meaningful for players; always {@code 0} for villagers. */
    private long joinTimestamp = 0;

    /** A villager's cached display name. Always {@code null} for players. */
    private String cachedName;

    /** A villager's cached origin (root) ID. Always {@code null} for players. */
    private String cachedRootId;

    /** A villager's cached resolved display title, as of the last census sweep. Always {@code null} for players. */
    private FactionTitle cachedTitle;

    /**
     * Private constructor — use {@link #createPlayer} or
     * {@link #createVillager} instead.
     *
     * @param uuid this participant's UUID
     * @param player whether this participant is a player
     */
    private FactionParticipant(UUID uuid, boolean player) {
        this.uuid = uuid;
        this.player = player;
    }

    /**
     * Creates a new player participant.
     *
     * @param uuid the player's UUID
     * @param joinTimestamp when they joined, in milliseconds since the epoch
     * @return the new participant
     */
    public static FactionParticipant createPlayer(UUID uuid, long joinTimestamp) {
        FactionParticipant participant = new FactionParticipant(uuid, true);
        participant.joinTimestamp = joinTimestamp;
        return participant;
    }

    /**
     * Creates a new villager participant.
     *
     * @param uuid the villager's UUID
     * @param name the villager's display name
     * @param rootId the villager's origin (root) ID
     * @param title the villager's resolved display title
     * @return the new participant
     */
    public static FactionParticipant createVillager(UUID uuid, String name, String rootId, FactionTitle title) {
        FactionParticipant participant = new FactionParticipant(uuid, false);
        participant.cachedName = name;
        participant.cachedRootId = rootId;
        participant.cachedTitle = title;
        return participant;
    }

    /**
     * Returns this participant's UUID.
     *
     * @return the UUID
     */
    public UUID getUUID() {
        return this.uuid;
    }

    /**
     * Checks whether this participant is a player.
     *
     * @return {@code true} if a player, {@code false} if a villager
     */
    public boolean isPlayer() {
        return this.player;
    }

    /**
     * Checks whether this participant is a villager.
     *
     * @return {@code true} if a villager, {@code false} if a player
     */
    public boolean isVillager() {
        return !this.player;
    }

    /**
     * Checks whether this participant currently holds a Leader role.
     *
     * @return {@code true} if currently a leader
     */
    public boolean isLeader() {
        return this.leader;
    }

    /**
     * Sets whether this participant currently holds a Leader role.
     *
     * @param leader the new leader status
     */
    public void setLeader(boolean leader) {
        this.leader = leader;
    }

    /**
     * Returns when this participant joined. Only meaningful for players.
     *
     * @return the join time, in milliseconds since the epoch; {@code 0} for villagers
     */
    public long getJoinTimestamp() {
        return this.joinTimestamp;
    }

    /**
     * Returns this villager's cached display name.
     *
     * @return the cached name, or {@code null} if this participant is a player
     */
    public String getCachedName() {
        return this.player ? null : this.cachedName;
    }

    /**
     * Returns this villager's cached origin (root) ID.
     *
     * @return the cached root ID, or {@code null} if this participant is a player
     */
    public String getCachedRootId() {
        return this.player ? null : this.cachedRootId;
    }

    /**
     * Returns this villager's cached resolved display title.
     *
     * @return the cached title, or {@code null} if this participant is a player
     */
    public FactionTitle getCachedTitle() {
        return this.player ? null : this.cachedTitle;
    }

    /**
     * Updates this villager's cached display fields. Does nothing if this
     * participant is a player.
     *
     * @param name the villager's current display name
     * @param rootId the villager's current origin (root) ID
     * @param title the villager's current resolved display title
     */
    public void updateVillagerCache(String name, String rootId, FactionTitle title) {
        if (this.player) return;
        this.cachedName = name;
        this.cachedRootId = rootId;
        this.cachedTitle = title;
    }
}