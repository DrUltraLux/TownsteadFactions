package com.drultralux.townsteadfactions.network;

/**
 * Named constants for the {@code action} values used to route
 * {@link com.drultralux.townsteadfactions.network.payload.FactionS2CPayload}
 * and
 * {@link com.drultralux.townsteadfactions.network.payload.FactionC2SPayload}
 * to their handlers. Using constants here instead of inline string
 * literals avoids typos between where an action is sent and where it's
 * registered.
 */
public final class FactionPacketActions {

    private FactionPacketActions() {}

    /** S2C: full faction state sync (assigned faction, resources, global roster). */
    public static final String FACTION_SYNC = "faction_sync";
    /** S2C: a single faction's updated state (id, resources, roster). */
    public static final String FACTION_SYNC_DELTA = "faction_sync_delta";
    /** S2C: instructs the client to discard its saved dashboard layout and rebuild defaults next time it's opened. */
    public static final String FACTION_LAYOUT_RESET = "faction_layout_reset";
    /** C2S: request older activity log entries for a faction, before a given timestamp. */
    public static final String FACTION_LOG_REQUEST_MORE = "faction_log_request_more";
    /** S2C: a batch of older activity log entries for a faction, in response to a request. */
    public static final String FACTION_LOG_MORE = "faction_log_more";
}