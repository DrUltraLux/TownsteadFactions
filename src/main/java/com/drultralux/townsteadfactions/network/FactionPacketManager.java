package com.drultralux.townsteadfactions.network;

import com.drultralux.townsteadfactions.factions.FactionManager;
import com.drultralux.townsteadfactions.factions.FactionParticipant;
import com.drultralux.townsteadfactions.factions.FactionTitle;
import com.drultralux.townsteadfactions.factions.TitleManager;
import com.drultralux.townsteadfactions.factions.voting.LeadershipManager;
import com.drultralux.townsteadfactions.factions.voting.VoteManager;
import com.drultralux.townsteadfactions.factions.voting.VoteRecord;
import com.drultralux.townsteadfactions.integration.optional.CapitalsIntegration;
import com.drultralux.townsteadfactions.integration.required.OriginManager;
import com.drultralux.townsteadfactions.network.payload.FactionS2CPayload;
import com.drultralux.townsteadfactions.territory.VillageControlManager;
import com.drultralux.townsteadfactions.territory.VillageMapManager;
import com.drultralux.townsteadfactions.layout.LayoutResetManager;
import com.drultralux.townsteadfactions.factions.ActivityLogEntry;
import java.util.List;
import java.util.Map;

import com.drultralux.townsteadfactions.utils.LogManager;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.UUID;

/**
 * Builds and sends faction data to clients over the generic
 * {@link FactionS2CPayload} channel.
 */
public class FactionPacketManager {

    /**
     * Sends a tagged payload to a single player. Reusable entry point for
     * any feature that needs to send server-to-client faction data —
     * builds the {@link FactionS2CPayload} and dispatches it.
     *
     * @param player the player to send to; does nothing if {@code null}
     * @param action the action name the client should route this data to
     * @param data the payload data
     */
    public static void sendToPlayer(ServerPlayer player, String action, CompoundTag data) {
        if (player == null) return;
        PacketDistributor.sendToPlayer(player, new FactionS2CPayload(action, data));
    }

    /**
     * Serializes the receiving player's assigned faction and resources,
     * plus a full snapshot of every active faction on the server, then
     * sends it tagged as {@link FactionPacketActions#FACTION_SYNC}.
     *
     * @param player the player to send faction data to
     */
    public static void sendFactionDataToClient(ServerPlayer player) {
        if (player == null) return;
        UUID playerUUID = player.getUUID();
        MinecraftServer server = player.getServer();

        String factionId = FactionManager.getPlayerFactionId(playerUUID);

        CompoundTag nbt = new CompoundTag();
        nbt.putString("assignedFactionId", (factionId != null) ? factionId : "none");
        nbt.putInt("cogs", FactionManager.getPlayerFactionAsset(playerUUID, "cogs"));
        nbt.putInt("food", FactionManager.getPlayerFactionAsset(playerUUID, "food"));
        nbt.putInt("mana", FactionManager.getPlayerFactionAsset(playerUUID, "mana"));
        nbt.putInt("serverLayoutResetVersion", LayoutResetManager.getGlobalResetVersion());

        CompoundTag factionsTag = new CompoundTag();
        for (String id : FactionManager.getActiveFactionIds()) {
            factionsTag.put(id, buildFactionSnapshot(id, server));
        }
        nbt.put("factions", factionsTag);

        sendToPlayer(player, FactionPacketActions.FACTION_SYNC, nbt);
    }

    /**
     * Sends a single faction's updated state to every currently connected
     * player, tagged as {@link FactionPacketActions#FACTION_SYNC_DELTA}.
     * Built once and sent identically to everyone — vote entries only
     * ever carry who's participated (not what they chose) and who's
     * eligible, both of which are safe to share identically, so no
     * per-recipient rebuild is needed.
     *
     * @param factionId the faction whose data changed
     * @param server the server, used to resolve member usernames/origins and the player list
     */
    public static void broadcastFactionDelta(String factionId, MinecraftServer server) {
        if (factionId == null || server == null) return;

        CompoundTag payload = new CompoundTag();
        payload.put("faction", buildFactionSnapshot(factionId, server));

        for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
            sendToPlayer(onlinePlayer, FactionPacketActions.FACTION_SYNC_DELTA, payload);
        }
    }

    /**
     * Builds and sends the next older batch of a faction's activity log
     * to a player, in response to a {@link FactionPacketActions#FACTION_LOG_REQUEST_MORE}
     * request.
     *
     * @param player the requesting player
     * @param factionId the faction to fetch history for
     * @param beforeTimestamp only entries strictly older than this are returned
     */
    public static void sendMoreActivityLog(ServerPlayer player, String factionId, long beforeTimestamp) {
        if (player == null || factionId == null) return;

        List<ActivityLogEntry> entries = FactionManager.getActivityLogBefore(factionId, beforeTimestamp, 20);

        CompoundTag responseNbt = new CompoundTag();
        responseNbt.putString("factionId", factionId);

        ListTag entriesList = new ListTag();
        for (ActivityLogEntry entry : entries) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putLong("timestamp", entry.timestamp());
            entryTag.putString("message", entry.message());
            entriesList.add(entryTag);
        }
        responseNbt.put("entries", entriesList);

        boolean hasMore = !entries.isEmpty() && !FactionManager.getActivityLogBefore(factionId, entries.get(entries.size() - 1).timestamp(), 1).isEmpty();
        responseNbt.putBoolean("hasMore", hasMore);

        sendToPlayer(player, FactionPacketActions.FACTION_LOG_MORE, responseNbt);
    }

    /**
     * Builds and sends a single controlled village's map snapshot (name,
     * world coordinates, and vanilla-format color data) to a player, in
     * response to a {@link FactionPacketActions#FACTION_VILLAGE_MAP_REQUEST}.
     * Villages are addressed by index into the faction's sorted controlled-
     * village list (see {@link VillageControlManager#getControlledVillageKeys})
     * rather than by key directly, so the client's prev/next arrows can
     * simply request index-1/index+1 without needing to know village keys
     * itself.
     *
     * @param player the requesting player
     * @param factionId the faction whose controlled villages to page through
     * @param index the index into that faction's controlled-village list
     */
    public static void sendVillageMap(ServerPlayer player, String factionId, int index) {
        if (player == null || factionId == null) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;

        List<String> villageKeys = VillageControlManager.getControlledVillageKeys(factionId);
        CompoundTag responseNbt = new CompoundTag();
        responseNbt.putString("factionId", factionId);
        responseNbt.putInt("total", villageKeys.size());

        if (villageKeys.isEmpty()) {
            responseNbt.putBoolean("found", false);
            sendToPlayer(player, FactionPacketActions.FACTION_VILLAGE_MAP_RESPONSE, responseNbt);
            return;
        }

        int clampedIndex = Math.floorMod(index, villageKeys.size());
        String villageKey = villageKeys.get(clampedIndex);
        VillageMapManager.ResolvedVillage resolved = VillageMapManager.resolve(server, villageKey);

        VillageControlManager.CachedVillageMap cached = VillageControlManager.getCachedVillageMap(villageKey);
        VillageMapManager.MapSnapshotResult snapshot = (resolved != null)
                ? VillageMapManager.generateColorSnapshot(resolved.level(), resolved.village())
                : null;

        if (snapshot != null && snapshot.isReliable()) {
            var center = resolved.village().getCenter();
            String name = resolved.village().getName();
            String dimension = resolved.level().dimension().location().toString();
            VillageControlManager.cacheVillageMap(villageKey, name, center.getX(), center.getZ(), dimension, snapshot.colors());
            LogManager.debug("Village map request for '" + villageKey + "' served live and cached (" + snapshot.noneCount() + " air pixels).");

            responseNbt.putBoolean("found", true);
            responseNbt.putBoolean("stale", false);
            responseNbt.putInt("index", clampedIndex);
            responseNbt.putString("name", name);
            responseNbt.putInt("x", center.getX());
            responseNbt.putInt("z", center.getZ());
            responseNbt.putString("dimension", dimension);
            responseNbt.put("colors", new ByteArrayTag(snapshot.colors()));
            sendToPlayer(player, FactionPacketActions.FACTION_VILLAGE_MAP_RESPONSE, responseNbt);
            return;
        }

        if (cached != null) {
            String reason = (snapshot == null) ? "village not resolvable live" : "live snapshot unreliable (" + snapshot.noneCount() + " air pixels)";
            LogManager.debug("Village map request for '" + villageKey + "' served from cached fallback (" + reason + ").");

            responseNbt.putBoolean("found", true);
            responseNbt.putBoolean("stale", true);
            responseNbt.putInt("index", clampedIndex);
            responseNbt.putString("name", cached.name());
            responseNbt.putInt("x", cached.x());
            responseNbt.putInt("z", cached.z());
            responseNbt.putString("dimension", cached.dimension());
            responseNbt.put("colors", new ByteArrayTag(cached.colors()));
            sendToPlayer(player, FactionPacketActions.FACTION_VILLAGE_MAP_RESPONSE, responseNbt);
            return;
        }

        if (snapshot != null) {
            // No cache exists yet to fall back on, and this snapshot is unreliable — send it anyway
            // (better than nothing on a first-ever view), but deliberately don't cache it, so it can't
            // poison the cache for future requests.
            var center = resolved.village().getCenter();
            String name = resolved.village().getName();
            String dimension = resolved.level().dimension().location().toString();
            LogManager.debug("Village map request for '" + villageKey + "' served live but UNCACHED — unreliable (" + snapshot.noneCount() + " air pixels) with no prior cache to fall back on.");

            responseNbt.putBoolean("found", true);
            responseNbt.putBoolean("stale", false);
            responseNbt.putInt("index", clampedIndex);
            responseNbt.putString("name", name);
            responseNbt.putInt("x", center.getX());
            responseNbt.putInt("z", center.getZ());
            responseNbt.putString("dimension", dimension);
            responseNbt.put("colors", new ByteArrayTag(snapshot.colors()));
            sendToPlayer(player, FactionPacketActions.FACTION_VILLAGE_MAP_RESPONSE, responseNbt);
            return;
        }

        LogManager.debug("Village map request for '" + villageKey + "' failed: village not resolvable live, and no cached fallback exists.");
        responseNbt.putBoolean("found", false);
        sendToPlayer(player, FactionPacketActions.FACTION_VILLAGE_MAP_RESPONSE, responseNbt);
    }

    /**
     * Builds an NBT snapshot of a single faction's ID, display name,
     * resources, member/villager rosters, activity log, and every
     * currently active leadership vote. Identical for every recipient —
     * vote entries expose who's currently eligible and who's already
     * participated, but never what anyone actually chose, so choices stay
     * fully private while still avoiding a per-recipient rebuild.
     *
     * @param factionId the faction to snapshot
     * @param server the server, used to resolve member usernames/origins/votes; may be {@code null}
     * @return the faction's snapshot as an NBT compound tag
     */
    private static CompoundTag buildFactionSnapshot(String factionId, MinecraftServer server) {
        CompoundTag snapshot = new CompoundTag();
        snapshot.putString("id", factionId);
        String displayName = FactionManager.getFactionDisplayName(factionId);
        snapshot.putString("displayName", (displayName != null) ? displayName : factionId);
        snapshot.putInt("cogs", FactionManager.getFactionAsset(factionId, "cogs"));
        snapshot.putInt("food", FactionManager.getFactionAsset(factionId, "food"));
        snapshot.putInt("mana", FactionManager.getFactionAsset(factionId, "mana"));
        snapshot.putInt("villagerCount", FactionManager.getVillagerCountForFaction(factionId));
        snapshot.putInt("controlledVillages", VillageControlManager.getControlledVillageCount(factionId));
        snapshot.putBoolean("capitalsFunctional", CapitalsIntegration.isIntegrationFunctional());

        List<ActivityLogEntry> recentLog = FactionManager.getRecentActivityLog(factionId, 30);
        ListTag activityLogList = new ListTag();
        for (ActivityLogEntry entry : recentLog) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putLong("timestamp", entry.timestamp());
            entryTag.putString("message", entry.message());
            activityLogList.add(entryTag);
        }
        snapshot.put("activityLog", activityLogList);
        snapshot.putBoolean("hasMoreLogHistory", !recentLog.isEmpty() && !FactionManager.getActivityLogBefore(factionId, recentLog.get(recentLog.size() - 1).timestamp(), 1).isEmpty());

        ListTag membersList = new ListTag();
        for (UUID memberUuid : FactionManager.getFactionMemberUUIDs(factionId)) {
            CompoundTag memberTag = new CompoundTag();
            ServerPlayer onlineMember = (server != null) ? server.getPlayerList().getPlayer(memberUuid) : null;

            memberTag.putUUID("uuid", memberUuid);
            memberTag.putString("name", resolveUsername(memberUuid, server));
            memberTag.putString("root", OriginManager.getDisplayRootName(memberUuid, onlineMember));
            memberTag.putString("title", FactionManager.isLeader(factionId, memberUuid) ? FactionTitle.LEADER.getDisplayName() : TitleManager.getResolvedTitleName(memberUuid, FactionTitle.MEMBER));
            memberTag.putBoolean("isLeader", FactionManager.isLeader(factionId, memberUuid));
            membersList.add(memberTag);
        }
        snapshot.put("members", membersList);

        ListTag villagerRosterList = new ListTag();
        for (Map.Entry<UUID, FactionParticipant> entry : FactionManager.getVillagersForFaction(factionId).entrySet()) {
            FactionParticipant participant = entry.getValue();
            CompoundTag villagerTag = new CompoundTag();
            villagerTag.putUUID("uuid", entry.getKey());
            villagerTag.putString("name", participant.getCachedName());
            villagerTag.putString("root", participant.getCachedRootId());
            villagerTag.putString("title", participant.isLeader() ? FactionTitle.LEADER.getDisplayName() : participant.getCachedTitle().getDisplayName());
            villagerTag.putBoolean("isLeader", participant.isLeader());
            villagerRosterList.add(villagerTag);
        }
        snapshot.put("villagerRoster", villagerRosterList);

        ListTag votesList = new ListTag();
        for (VoteRecord vote : VoteManager.getActiveVotesForFaction(factionId)) {
            votesList.add(buildVoteSnapshot(vote, server));
        }
        snapshot.put("activeVotes", votesList);

        return snapshot;
    }

    /**
     * Builds an NBT snapshot of a single active vote: its identity, the
     * target's resolved display name/origin, timing, the current public
     * tally, and the UUIDs of everyone eligible / everyone who's already
     * cast. Deliberately never includes what anyone actually chose — only
     * that a given UUID has participated — so a client can check its own
     * status locally while every other player's choice, and even whether
     * they've voted at all, is never rendered anywhere.
     *
     * @param vote the vote to snapshot
     * @param server the server, used to resolve the target's identity and the tally
     * @return the vote's snapshot as an NBT compound tag
     */
    private static CompoundTag buildVoteSnapshot(VoteRecord vote, MinecraftServer server) {
        CompoundTag voteTag = new CompoundTag();
        voteTag.putUUID("voteId", vote.getVoteId());
        voteTag.putString("type", vote.getType().name());
        voteTag.putUUID("targetUUID", vote.getTargetUUID());
        voteTag.putString("targetName", resolveParticipantDisplayName(vote.getFactionId(), vote.getTargetUUID(), server));
        voteTag.putString("targetRoot", resolveParticipantDisplayRoot(vote.getFactionId(), vote.getTargetUUID(), server));
        voteTag.putLong("expiryTimestamp", vote.getExpiryTimestamp());

        LeadershipManager.PublicTally tally = LeadershipManager.getPublicTally(vote, server);
        voteTag.putInt("yesCount", tally.yes());
        voteTag.putInt("noCount", tally.no());
        voteTag.putInt("totalEligibleVoters", tally.totalEligible());

        ListTag votedUUIDs = new ListTag();
        for (UUID votedPlayer : vote.getPlayerVotes().keySet()) {
            votedUUIDs.add(net.minecraft.nbt.NbtUtils.createUUID(votedPlayer));
        }
        voteTag.put("votedUUIDs", votedUUIDs);

        ListTag eligibleUUIDs = new ListTag();
        for (UUID eligibleParticipant : LeadershipManager.getEligibleVoterUUIDs(vote, server)) {
            eligibleUUIDs.add(net.minecraft.nbt.NbtUtils.createUUID(eligibleParticipant));
        }
        voteTag.put("eligibleUUIDs", eligibleUUIDs);

        return voteTag;
    }

    /**
     * Resolves a participant's display name for use in a vote snapshot —
     * a villager's cached name, or a player's resolved username.
     *
     * @param factionId the faction the participant belongs to
     * @param uuid the participant to resolve
     * @param server the server, used to resolve an online/offline player's name
     * @return the resolved display name
     */
    private static String resolveParticipantDisplayName(String factionId, UUID uuid, MinecraftServer server) {
        for (FactionParticipant participant : FactionManager.getAllParticipants(factionId)) {
            if (participant.getUUID().equals(uuid)) {
                if (participant.isVillager()) return participant.getCachedName();
                return resolveUsername(uuid, server);
            }
        }
        return "Unknown";
    }

    /**
     * Resolves a participant's display origin for use in a vote snapshot
     * — a villager's cached origin, or a player's currently-known origin.
     *
     * @param factionId the faction the participant belongs to
     * @param uuid the participant to resolve
     * @param server the server, used to resolve an online player
     * @return the resolved origin display name
     */
    private static String resolveParticipantDisplayRoot(String factionId, UUID uuid, MinecraftServer server) {
        for (FactionParticipant participant : FactionManager.getAllParticipants(factionId)) {
            if (participant.getUUID().equals(uuid)) {
                if (participant.isVillager()) return participant.getCachedRootId();
                ServerPlayer online = (server != null) ? server.getPlayerList().getPlayer(uuid) : null;
                return OriginManager.getDisplayRootName(uuid, online);
            }
        }
        return "Unknown";
    }

    /**
     * Resolves a player's display name from their UUID, preferring the
     * currently online player, then falling back to the server's profile
     * cache for offline members.
     *
     * @param memberUuid the UUID to resolve
     * @param server the server to resolve against; may be {@code null}
     * @return the resolved username, or {@code "Unknown Member"} if it can't be resolved
     */
    private static String resolveUsername(UUID memberUuid, MinecraftServer server) {
        return FactionManager.resolvePlayerDisplayName(server, memberUuid);
    }
}