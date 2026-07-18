package com.drultralux.townsteadfactions.network;

import com.drultralux.townsteadfactions.factions.FactionManager;
import com.drultralux.townsteadfactions.network.payload.FactionS2CPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
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
     * Far cheaper than a full re-sync when only one faction's data has
     * changed.
     *
     * @param factionId the faction whose data changed
     * @param server the server, used to resolve member usernames and the player list
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
     * Builds an NBT snapshot of a single faction's ID, display name,
     * resources, and member display names.
     *
     * @param factionId the faction to snapshot
     * @param server the server, used to resolve member usernames; may be {@code null}
     * @return the faction's snapshot as an NBT compound tag
     */
    private static CompoundTag buildFactionSnapshot(String factionId, MinecraftServer server) {
        CompoundTag snapshot = new CompoundTag();
        snapshot.putString("id", factionId);
        snapshot.putString("displayName", FactionManager.getFactionDisplayName(factionId));
        snapshot.putInt("cogs", FactionManager.getFactionAsset(factionId, "cogs"));
        snapshot.putInt("food", FactionManager.getFactionAsset(factionId, "food"));
        snapshot.putInt("mana", FactionManager.getFactionAsset(factionId, "mana"));

        ListTag namesList = new ListTag();
        for (UUID memberUuid : FactionManager.getFactionMemberUUIDs(factionId)) {
            namesList.add(StringTag.valueOf(resolveUsername(memberUuid, server)));
        }
        snapshot.put("members", namesList);
        return snapshot;
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
        if (memberUuid == null || server == null) return "Unknown Member";

        ServerPlayer onlineMember = server.getPlayerList().getPlayer(memberUuid);
        if (onlineMember != null) {
            return onlineMember.getName().getString();
        }
        var profile = server.getProfileCache().get(memberUuid);
        return profile.map(com.mojang.authlib.GameProfile::getName).orElse("Unknown Member");
    }
}