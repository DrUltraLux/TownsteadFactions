package com.drultralux.townsteadfactions.factions.voting;

import com.drultralux.townsteadfactions.config.ModConfig;
import com.drultralux.townsteadfactions.factions.FactionManager;
import com.drultralux.townsteadfactions.factions.FactionParticipant;
import com.drultralux.townsteadfactions.factions.FactionTitle;
import com.drultralux.townsteadfactions.integration.optional.CapitalsIntegration;
import com.drultralux.townsteadfactions.network.FactionPacketManager;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

/**
 * Periodically scans every faction for participants (players or
 * villagers) who currently hold real Monarch rank via Capitals but
 * aren't yet a Leader. The first two Monarchs a faction has as leaders
 * are elevated automatically, with no vote; any further Monarch instead
 * has an {@link VoteType#ELECT_MONARCH} vote started for them, gated to
 * the faction's existing Noble+Monarch-tier voters.
 *
 * <p>Does nothing at all if Capitals isn't present or its integration
 * isn't currently functional — factions fall back to fully democratic
 * leadership in that case, exactly as if Capitals weren't installed.</p>
 */
public final class MonarchElevationTicker {

    /** Server ticks elapsed since the last sweep. */
    private static int ticksSinceLastSweep = 0;

    private MonarchElevationTicker() {}

    /**
     * Checks whether it's time for the next sweep and, if so, scans every
     * active faction for un-elevated Monarchs.
     *
     * @param event the server tick event
     */
    public static void onServerTick(ServerTickEvent.Post event) {
        int intervalSeconds = ModConfig.COMMON.getInteger("monarchElevationCheckIntervalSeconds", 60);
        int intervalTicks = Math.max(1, intervalSeconds * 20);

        ticksSinceLastSweep++;
        if (ticksSinceLastSweep < intervalTicks) return;
        ticksSinceLastSweep = 0;

        if (!CapitalsIntegration.isIntegrationFunctional()) return;

        MinecraftServer server = event.getServer();
        for (String factionId : FactionManager.getActiveFactionIds()) {
            processFactionMonarchs(factionId, server);
        }
    }

    /**
     * Processes a single faction: counts its current Monarch-rank
     * leaders, then either auto-elevates or starts a vote for each
     * not-yet-leader Monarch found, depending on whether the two free
     * slots are already filled.
     *
     * @param factionId the faction to process
     * @param server the server, used to broadcast changes and check vote eligibility
     */
    private static void processFactionMonarchs(String factionId, MinecraftServer server) {
        List<FactionParticipant> participants = FactionManager.getAllParticipants(factionId);

        int currentMonarchLeaderCount = 0;
        for (FactionParticipant participant : participants) {
            if (participant.isLeader() && CapitalsIntegration.resolveTitle(participant.getUUID()) == FactionTitle.MONARCH) {
                currentMonarchLeaderCount++;
            }
        }

        for (FactionParticipant participant : participants) {
            if (participant.isLeader()) continue; // already a leader, nothing to do

            FactionTitle rank = CapitalsIntegration.resolveTitle(participant.getUUID());
            if (rank != FactionTitle.MONARCH) continue;

            if (currentMonarchLeaderCount < 2) {
                FactionManager.setLeader(factionId, participant.getUUID(), true);
                FactionManager.logFactionAction(factionId, FactionManager.getParticipantDisplayName(factionId, participant.getUUID(), server) + " was automatically elevated to Leader as a Monarch.");
                if (server != null) {
                    FactionPacketManager.broadcastFactionDelta(factionId, server);
                }
                currentMonarchLeaderCount++;
            } else {
                // startVote already no-ops if an identical vote is already active,
                // so it's safe to call this every sweep without our own duplicate tracking.
                LeadershipManager.startVote(factionId, VoteType.ELECT_MONARCH, participant.getUUID(), null, server);
            }
        }
    }
}