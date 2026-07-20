package com.drultralux.townsteadfactions.factions.voting;

/**
 * A single voter's choice on an active leadership vote.
 */
public enum VoteChoice {

    /** Support for the vote's outcome (accepting a candidate, or removing a leader). */
    YES,

    /** Opposition to the vote's outcome. */
    NO,

    /**
     * Neither support nor opposition. Abstaining reduces the total number
     * of votes counted toward the threshold, since thresholds are
     * computed against votes actually cast (yes + no), not the full
     * eligible pool — so a voter who genuinely opposes an outcome should
     * vote {@link #NO}, not abstain.
     */
    ABSTAIN
}