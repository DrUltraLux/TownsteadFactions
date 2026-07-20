package com.drultralux.townsteadfactions.factions.voting;

import com.drultralux.townsteadfactions.config.ModConfig;
import com.drultralux.townsteadfactions.factions.FactionManager;
import com.drultralux.townsteadfactions.factions.FactionParticipant;
import com.drultralux.townsteadfactions.factions.FactionTitle;
import com.drultralux.townsteadfactions.integration.optional.CapitalsIntegration;
import com.drultralux.townsteadfactions.integration.required.OriginManager;
import com.drultralux.townsteadfactions.network.FactionPacketManager;
import com.drultralux.townsteadfactions.utils.LogManager;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.entity.ai.relationship.AgeState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.UUID;

/**
 * Orchestrates faction leadership voting: eligibility, tallying
 * (including live-computed villager participation via MCA hearts),
 * threshold resolution, and applying an outcome. Pure storage lives in
 * {@link VoteManager}; this class is the business logic layered on top.
 *
 * <p>Villager voting choices are never stored — they're recomputed fresh
 * from current hearts every time a vote is tallied, since opinion can
 * shift over a vote's multi-day lifetime.</p>
 */
public final class LeadershipManager {

    /** Server ticks elapsed since the last periodic vote sweep. */
    private static int ticksSinceLastSweep = 0;

    private LeadershipManager() {}

    /**
     * Periodically sweeps every active vote across every faction,
     * resolving any that have crossed their threshold or expired. Runs at
     * a config-driven interval — this is what catches a vote that becomes
     * decisive purely from villager opinion drifting, with no new player
     * action to trigger an immediate check.
     *
     * @param event the server tick event
     */
    public static void onServerTick(ServerTickEvent.Post event) {
        int intervalSeconds = ModConfig.COMMON.getInteger("leadershipVoteCheckIntervalSeconds", 300);
        int intervalTicks = Math.max(1, intervalSeconds * 20);

        ticksSinceLastSweep++;
        if (ticksSinceLastSweep < intervalTicks) return;
        ticksSinceLastSweep = 0;

        MinecraftServer server = event.getServer();
        for (VoteRecord vote : VoteManager.getAllActiveVotes()) {
            checkAndResolveVote(vote, server);
        }
    }

    /**
     * Starts a new leadership vote, if there isn't already an active vote
     * of the same type concerning the same target in that faction.
     *
     * @param factionId the faction this vote belongs to
     * @param type the kind of vote to start
     * @param targetUUID the vote's subject (candidate or leader-to-remove)
     * @param nominatorUUID the player who started this vote, or {@code null} if not applicable
     * @param server the server, used to broadcast the resulting change
     * @return the newly created vote, or {@code null} if invalid or a duplicate already exists
     */
    public static VoteRecord startVote(String factionId, VoteType type, UUID targetUUID, UUID nominatorUUID, MinecraftServer server) {
        if (factionId == null || type == null || targetUUID == null) return null;

        for (VoteRecord existing : VoteManager.getActiveVotesForFaction(factionId)) {
            if (existing.getType() == type && existing.getTargetUUID().equals(targetUUID)) {
                return null; // an identical vote is already active — don't start a duplicate
            }
        }

        long durationMillis = ModConfig.COMMON.getInteger("voteDurationHours", 48) * 3_600_000L;
        VoteRecord record = VoteManager.startVote(factionId, type, targetUUID, nominatorUUID, durationMillis);

        FactionManager.logFactionAction(factionId, "A " + describeVoteType(type) + " vote was started concerning " + FactionManager.getParticipantDisplayName(factionId, targetUUID, server) + ".");
        if (server != null) {
            FactionPacketManager.broadcastFactionDelta(factionId, server);
        }
        return record;
    }

    /**
     * Casts a player's vote, if they're currently eligible, then
     * immediately re-tallies — so a vote that becomes decisive resolves
     * right away rather than waiting for the next periodic sweep.
     *
     * @param voteId the vote to cast on
     * @param voterUUID the voting player
     * @param choice the choice to cast
     * @param server the server, used for eligibility checks and to broadcast a resulting change
     * @return {@code true} if the vote was accepted
     */
    public static boolean castVote(UUID voteId, UUID voterUUID, VoteChoice choice, MinecraftServer server) {
        VoteRecord vote = VoteManager.getVote(voteId);
        if (vote == null || voterUUID == null || choice == null) return false;
        if (!canVote(vote, voterUUID, server)) return false;

        VoteManager.castVote(voteId, voterUUID, choice);
        checkAndResolveVote(vote, server);
        return true;
    }

    /**
     * Checks whether a participant is currently eligible to vote on a
     * specific vote, evaluated fresh against their current rank/role —
     * never against a snapshot taken when the vote started.
     *
     * @param vote the vote to check eligibility for
     * @param voterUUID the participant to check
     * @param server the server, used to resolve online status for rank checks
     * @return {@code true} if currently eligible to vote
     */
    public static boolean canVote(VoteRecord vote, UUID voterUUID, MinecraftServer server) {
        if (vote == null || voterUUID == null) return false;
        String factionId = vote.getFactionId();

        return switch (vote.getType()) {
            case DEMOTE -> FactionManager.isLeader(factionId, voterUUID);
            case ELECT_MONARCH -> {
                if (!CapitalsIntegration.isIntegrationFunctional()) yield false;
                FactionTitle rank = CapitalsIntegration.resolveTitle(voterUUID);
                yield rank == FactionTitle.NOBLE || rank == FactionTitle.MONARCH;
            }
            case ELECT -> {
                FactionParticipant participant = findParticipant(factionId, voterUUID);
                if (participant == null) yield false;
                yield participant.isPlayer() || ModConfig.COMMON.getBoolean("villagerVotingEnabled", true);
            }
        };
    }

    /**
     * Tallies a vote and, if it has now crossed its passing threshold or
     * expired, applies the outcome and removes it from active storage.
     * Does nothing if the vote is neither decisive nor expired yet.
     *
     * @param vote the vote to check
     * @param server the server, used for eligibility/hearts checks and to broadcast a resulting change
     */
    public static void checkAndResolveVote(VoteRecord vote, MinecraftServer server) {
        Tally tally = tallyVote(vote, server);

        if (hasPassed(vote, tally)) {
            applyOutcome(vote, true, server);
            return;
        }

        if (vote.isExpired(System.currentTimeMillis())) {
            applyOutcome(vote, false, server);
        }
    }

    /**
     * Checks whether a player is currently allowed to self-nominate for
     * leadership: Capitals must not be functionally present (Monarchs are
     * fully automatic and never need this), they must be a current
     * faction member, not already a leader, and not already have an
     * identical self-nomination vote active.
     *
     * @param factionId the player's faction
     * @param playerUUID the player to check
     * @return {@code true} if they can currently click "Request leadership position"
     */
    public static boolean canRequestLeadership(String factionId, UUID playerUUID) {
        if (factionId == null || playerUUID == null) return false;
        if (CapitalsIntegration.isIntegrationFunctional()) return false;

        FactionParticipant participant = findParticipant(factionId, playerUUID);
        if (participant == null || participant.isLeader()) return false;

        if (getCurrentLeaderCount(factionId) >= getMaxLeaderCount(factionId)) return false;

        for (VoteRecord existing : VoteManager.getActiveVotesForFaction(factionId)) {
            if (existing.getType() == VoteType.ELECT && existing.getTargetUUID().equals(playerUUID)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Starts a self-nomination vote for a player, if {@link #canRequestLeadership}
     * currently allows it. This is the server-side authority for the
     * "Request leadership position" button — the client only ever decides
     * whether to *show* that option; this method is what actually
     * enforces it's legitimate.
     *
     * @param factionId the player's faction
     * @param playerUUID the requesting player
     * @param server the server, used to broadcast the resulting change
     * @return the newly created vote, or {@code null} if not currently eligible
     */
    public static VoteRecord requestLeadership(String factionId, UUID playerUUID, MinecraftServer server) {
        if (!canRequestLeadership(factionId, playerUUID)) return null;
        return startVote(factionId, VoteType.ELECT, playerUUID, playerUUID, server);
    }

    /**
     * Computes a vote's current public tally: yes/no counts, and the
     * total number of participants currently eligible to vote on it
     * (whether or not they've cast yet) — used to render vote progress in
     * the UI.
     *
     * @param vote the vote to tally
     * @param server the server, used to resolve villager hearts and player rank
     * @return the public tally
     */
    public static PublicTally getPublicTally(VoteRecord vote, MinecraftServer server) {
        Tally tally = tallyVote(vote, server);
        int totalEligible = 0;
        for (FactionParticipant participant : FactionManager.getAllParticipants(vote.getFactionId())) {
            if (canVote(vote, participant.getUUID(), server)) totalEligible++;
        }
        return new PublicTally(tally.yes, tally.no, totalEligible);
    }

    /**
     * Returns the UUIDs of every participant currently eligible to vote
     * on a vote — players and villagers alike. Eligibility itself isn't
     * sensitive (it's derived from public rank/leadership/membership
     * facts), so this can be shared identically with every client; each
     * client only ever checks whether its own UUID appears in it.
     *
     * @param vote the vote to check
     * @param server the server, used for rank/hearts-based eligibility checks
     * @return the UUIDs of every currently-eligible voter
     */
    public static List<UUID> getEligibleVoterUUIDs(VoteRecord vote, MinecraftServer server) {
        List<UUID> result = new java.util.ArrayList<>();
        for (FactionParticipant participant : FactionManager.getAllParticipants(vote.getFactionId())) {
            if (canVote(vote, participant.getUUID(), server)) {
                result.add(participant.getUUID());
            }
        }
        return result;
    }

    /**
     * A vote's public-facing tally: yes/no counts, and the total number
     * of participants currently eligible to vote on it.
     *
     * @param yes the current yes count
     * @param no the current no count
     * @param totalEligible the total number of currently-eligible voters
     */
    public record PublicTally(int yes, int no, int totalEligible) {}

    /**
     * Tallies every currently-eligible voter's choice on a vote: real
     * cast choices for players, freshly-computed hearts-based choices for
     * villagers. Abstains (explicit or computed) aren't counted toward
     * either side.
     *
     * @param vote the vote to tally
     * @param server the server, used to resolve villager hearts and player rank
     * @return the current yes/no counts
     */
    private static Tally tallyVote(VoteRecord vote, MinecraftServer server) {
        Tally tally = new Tally();
        List<FactionParticipant> participants = FactionManager.getAllParticipants(vote.getFactionId());

        for (FactionParticipant participant : participants) {
            UUID uuid = participant.getUUID();
            if (!canVote(vote, uuid, server)) continue;

            VoteChoice choice = participant.isPlayer()
                    ? vote.getPlayerVotes().get(uuid)
                    : computeVillagerVote(server, participant, vote);

            if (choice == VoteChoice.YES) tally.yes++;
            else if (choice == VoteChoice.NO) tally.no++;
        }
        return tally;
    }

    /**
     * Computes a villager's live vote choice: non-adults never vote;
     * otherwise, effective hearts toward the vote's target (real hearts,
     * plus a same-origin bonus) are compared against the friend and
     * dislike thresholds to decide support, opposition, or abstention.
     *
     * @param server the server, used to find the villager's live entity
     * @param villagerVoter the villager casting this computed vote
     * @param vote the vote being tallied
     * @return the villager's current computed choice
     */
    private static VoteChoice computeVillagerVote(MinecraftServer server, FactionParticipant villagerVoter, VoteRecord vote) {
        VillagerEntityMCA liveEntity = findLiveVillager(server, villagerVoter.getUUID());
        if (liveEntity == null || liveEntity.getAgeState() != AgeState.ADULT) {
            return VoteChoice.ABSTAIN;
        }

        var memory = liveEntity.getVillagerBrain().getMemories().get(vote.getTargetUUID());
        int actualHearts = (memory != null) ? memory.getHearts() : 0;

        String targetRoot = resolveParticipantRoot(server, vote.getFactionId(), vote.getTargetUUID());
        boolean sameRoot = targetRoot != null && targetRoot.equals(villagerVoter.getCachedRootId());
        int effectiveHearts = actualHearts + (sameRoot ? 10 : 0);

        int friendThreshold = net.conczin.mca.Config.getInstance().heartsToBeConsideredAsFriend;
        int dislikeThreshold = ModConfig.COMMON.getInteger("villagerDislikeThreshold", 0);

        boolean supportsTarget;
        if (effectiveHearts >= friendThreshold) {
            supportsTarget = true;
        } else if (effectiveHearts < dislikeThreshold) {
            supportsTarget = false;
        } else {
            return VoteChoice.ABSTAIN;
        }

        boolean isElectType = vote.getType() != VoteType.DEMOTE;
        if (isElectType) {
            return supportsTarget ? VoteChoice.YES : VoteChoice.NO;
        } else {
            // Liking the target means opposing their removal (NO); disliking them supports it (YES).
            return supportsTarget ? VoteChoice.NO : VoteChoice.YES;
        }
    }

    /**
     * Checks whether a vote has crossed its passing threshold given the
     * current tally: a simple majority of votes cast for
     * {@link VoteType#ELECT}/{@link VoteType#ELECT_MONARCH}, or a 66%
     * supermajority for {@link VoteType#DEMOTE}.
     *
     * @param vote the vote being checked
     * @param tally the current tally
     * @return {@code true} if the vote currently passes
     */
    private static boolean hasPassed(VoteRecord vote, Tally tally) {
        int totalCast = tally.yes + tally.no;
        if (totalCast == 0) return false;

        if (vote.getType() == VoteType.DEMOTE) {
            return ((double) tally.yes / totalCast) >= 0.66;
        }
        return tally.yes > tally.no;
    }

    /**
     * Applies a vote's outcome: elevates or demotes the target if it
     * passed, logs the result either way, removes the vote from active
     * storage, and broadcasts the resulting change.
     *
     * @param vote the vote being resolved
     * @param passed whether it passed
     * @param server the server, used to broadcast the resulting change
     */
    private static void applyOutcome(VoteRecord vote, boolean passed, MinecraftServer server) {
        String factionId = vote.getFactionId();
        UUID target = vote.getTargetUUID();

        if (passed) {
            switch (vote.getType()) {
                case ELECT, ELECT_MONARCH -> {
                    FactionManager.setLeader(factionId, target, true);
                    FactionManager.logFactionAction(factionId, "Vote passed: " + FactionManager.getParticipantDisplayName(factionId, target, server) + " was elevated to Leader.");
                }
                case DEMOTE -> {
                    FactionManager.setLeader(factionId, target, false);
                    FactionManager.logFactionAction(factionId, "Vote passed: " + FactionManager.getParticipantDisplayName(factionId, target, server) + " was removed as Leader.");
                }
            }
        } else {
            FactionManager.logFactionAction(factionId, "A " + describeVoteType(vote.getType()) + " vote concerning " + FactionManager.getParticipantDisplayName(factionId, target, server) + " expired without passing.");
        }

        VoteManager.removeVote(vote.getVoteId());
        if (server != null) {
            FactionPacketManager.broadcastFactionDelta(factionId, server);
        }
    }

    /**
     * Resolves a participant's origin, for the same-origin voting bonus:
     * a villager's cached origin, or a player's currently-known origin
     * (live if online, best-effort cached otherwise).
     *
     * @param server the server, used to resolve an online player
     * @param factionId the faction the participant belongs to
     * @param uuid the participant to resolve
     * @return the resolved origin display name, or {@code null} if unresolvable
     */
    private static String resolveParticipantRoot(MinecraftServer server, String factionId, UUID uuid) {
        FactionParticipant participant = findParticipant(factionId, uuid);
        if (participant == null) return null;
        if (participant.isVillager()) return participant.getCachedRootId();

        ServerPlayer online = (server != null) ? server.getPlayerList().getPlayer(uuid) : null;
        return OriginManager.getDisplayRootName(uuid, online);
    }

    /**
     * Finds a specific participant within a faction's roster.
     *
     * @param factionId the faction to search
     * @param uuid the participant to find
     * @return the matching participant, or {@code null} if not found
     */
    private static FactionParticipant findParticipant(String factionId, UUID uuid) {
        for (FactionParticipant participant : FactionManager.getAllParticipants(factionId)) {
            if (participant.getUUID().equals(uuid)) {
                return participant;
            }
        }
        return null;
    }

    /**
     * Finds a villager's live entity by UUID across every loaded
     * dimension. Returns {@code null} if their chunk isn't currently
     * loaded — in which case their vote is simply not counted this pass,
     * consistent with how the census sweep already treats unloaded
     * villagers.
     *
     * @param server the server to search
     * @param uuid the villager to find
     * @return the live entity, or {@code null} if not currently loaded
     */
    private static VillagerEntityMCA findLiveVillager(MinecraftServer server, UUID uuid) {
        for (ServerLevel level : server.getAllLevels()) {
            var entity = level.getEntity(uuid);
            if (entity instanceof VillagerEntityMCA villager) {
                return villager;
            }
        }
        return null;
    }

    /**
     * Produces a short, human-readable name for a vote type, for log
     * messages.
     *
     * @param type the vote type to describe
     * @return the description
     */
    private static String describeVoteType(VoteType type) {
        return switch (type) {
            case ELECT -> "leadership election";
            case ELECT_MONARCH -> "Monarch acceptance";
            case DEMOTE -> "demotion";
        };
    }

    /**
     * Checks whether a leader is currently allowed to nominate a
     * different faction member for leadership: Capitals must not be
     * functionally present, the nominator must be a current leader, the
     * target must be a current, non-leader participant of the same
     * faction, and there mustn't already be an identical nomination
     * active.
     *
     * @param factionId the faction both belong to
     * @param nominatorUUID the leader doing the nominating
     * @param targetUUID the member being nominated
     * @return {@code true} if this nomination is currently allowed
     */
    public static boolean canNominateForLeadership(String factionId, UUID nominatorUUID, UUID targetUUID) {
        if (factionId == null || nominatorUUID == null || targetUUID == null) return false;
        if (CapitalsIntegration.isIntegrationFunctional()) return false;
        if (!FactionManager.isLeader(factionId, nominatorUUID)) return false;

        FactionParticipant target = findParticipant(factionId, targetUUID);
        if (target == null || target.isLeader()) return false;

        if (getCurrentLeaderCount(factionId) >= getMaxLeaderCount(factionId)) return false;

        for (VoteRecord existing : VoteManager.getActiveVotesForFaction(factionId)) {
            if (existing.getType() == VoteType.ELECT && existing.getTargetUUID().equals(targetUUID)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Starts a leadership vote for a different faction member, nominated
     * by a current leader, if {@link #canNominateForLeadership} currently
     * allows it. Server-side authority for the Leadership tab's
     * "Nominate" control.
     *
     * @param factionId the faction both belong to
     * @param nominatorUUID the leader doing the nominating
     * @param targetUUID the member being nominated
     * @param server the server, used to broadcast the resulting change
     * @return the newly created vote, or {@code null} if not currently allowed
     */
    public static VoteRecord nominateForLeadership(String factionId, UUID nominatorUUID, UUID targetUUID, MinecraftServer server) {
        if (!canNominateForLeadership(factionId, nominatorUUID, targetUUID)) return null;
        return startVote(factionId, VoteType.ELECT, targetUUID, nominatorUUID, server);
    }

    /**
     * Voluntarily resigns a leader from their role, immediately and
     * unilaterally — no vote involved, matching how any leader can always
     * step down on their own.
     *
     * @param factionId the leader's faction
     * @param playerUUID the resigning leader
     * @param server the server, used to broadcast the resulting change
     * @return {@code true} if they were a leader and have now resigned
     */
    public static boolean resignLeadership(String factionId, UUID playerUUID, MinecraftServer server) {
        if (factionId == null || playerUUID == null) return false;
        if (!FactionManager.isLeader(factionId, playerUUID)) return false;

        FactionManager.setLeader(factionId, playerUUID, false);
        FactionManager.logFactionAction(factionId, FactionManager.getParticipantDisplayName(factionId, playerUUID, server) + " voluntarily resigned as Leader.");
        if (server != null) {
            FactionPacketManager.broadcastFactionDelta(factionId, server);
        }
        return true;
    }

    /**
     * Computes the maximum number of leaders a faction is currently
     * allowed to have: 10% of its total participant count (players and
     * villagers combined), floored, with a minimum of 1 regardless of how
     * small the faction is.
     *
     * @param factionId the faction to check
     * @return the maximum allowed leader count
     */
    public static int getMaxLeaderCount(String factionId) {
        if (factionId == null) return 1;
        int totalParticipants = FactionManager.getAllParticipants(factionId).size();
        return Math.max(1, Math.round(totalParticipants / 10.0f));
    }

    /**
     * Counts how many participants currently hold a Leader role in a
     * faction.
     *
     * @param factionId the faction to check
     * @return the current leader count
     */
    public static int getCurrentLeaderCount(String factionId) {
        if (factionId == null) return 0;
        int count = 0;
        for (FactionParticipant participant : FactionManager.getAllParticipants(factionId)) {
            if (participant.isLeader()) count++;
        }
        return count;
    }

    /** A running yes/no tally for a vote; abstains are deliberately not tracked here at all. */
    private static class Tally {
        int yes = 0;
        int no = 0;
    }
}