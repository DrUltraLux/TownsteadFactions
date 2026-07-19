package com.drultralux.townsteadfactions.factions;

/**
 * A single entry in a faction's activity log.
 *
 * @param timestamp the real-world time this entry was recorded, in milliseconds since the epoch
 * @param message the human-readable description of what happened
 */
public record ActivityLogEntry(long timestamp, String message) {}