package com.drultralux.townsteadfactions.factions.voting;

/**
 * The kind of leadership vote in progress, determining who's eligible to
 * vote and what threshold is required to pass.
 */
public enum VoteType {

    /**
     * A general-membership vote to accept a candidate as a new faction
     * leader. Simple majority of votes cast (excluding abstains). All
     * members — and eligible villagers, if configured — may vote.
     */
    ELECT,

    /**
     * A vote to accept a 3rd or later Monarch into a faction's
     * leadership, once the first two auto-elevated slots are filled.
     * Simple majority of votes cast, but restricted to Noble+Monarch-tier
     * voters only (players and villagers alike).
     */
    ELECT_MONARCH,

    /**
     * A vote among a faction's existing leaders to remove one of them.
     * Requires a 66% supermajority of votes cast; the target may vote in
     * their own demotion.
     */
    DEMOTE
}