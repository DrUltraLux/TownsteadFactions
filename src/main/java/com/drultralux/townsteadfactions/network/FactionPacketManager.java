package com.drultralux.townsteadfactions.network;

import com.drultralux.townsteadfactions.client.FactionSyncPayload;
import com.drultralux.townsteadfactions.factions.Faction;
import com.drultralux.townsteadfactions.factions.FactionManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.Map;
import java.util.UUID;

/**
 * Centrally manages server-to-client data payload serialization and network transmission.
 * Packs and streams the comprehensive server-side faction matrix straight to player client screens.
 */
public class FactionPacketManager {

    /**
     * Serializes all live active server faction definitions and member rosters into a unified data payload,
     * then shoots it directly down the network stream channel to the target client player.
     *
     * @param player The target receiver server player connection handle
     */
    public static void sendFactionDataToClient(ServerPlayer player) {
        if (player == null) return;
        java.util.UUID playerUUID = player.getUUID();

        // Query assigned faction and primitives securely via our static helper accessors
        String factionId = FactionManager.getPlayerFactionId(playerUUID);

        CompoundTag nbt = new CompoundTag();
        nbt.putString("assignedFactionId", (factionId != null) ? factionId : "none");

        nbt.putInt("cogs", FactionManager.getPlayerFactionAsset(playerUUID, "cogs"));
        nbt.putInt("food", FactionManager.getPlayerFactionAsset(playerUUID, "food"));
        nbt.putInt("mana", FactionManager.getPlayerFactionAsset(playerUUID, "mana"));

        // Build the global rosters compound list map securely using only string keys
        CompoundTag rosterNbt = new CompoundTag();
        java.util.Set<String> activeFactionIds = FactionManager.getInstance().getActiveFactions().keySet();

        for (String id : activeFactionIds) {
            if (id != null) {
                net.minecraft.nbt.ListTag namesList = new net.minecraft.nbt.ListTag();
                net.minecraft.server.MinecraftServer server = player.getServer();

                // Pull membership rosters strictly inside a secure local loop context
                var membersList = FactionManager.getInstance().getActiveFactions().get(id).getMembers();
                if (membersList != null) {
                    for (java.util.UUID memberUuid : membersList) {
                        if (memberUuid != null) {
                            String username = "Unknown Member";
                            if (server != null) {
                                ServerPlayer onlineMember = server.getPlayerList().getPlayer(memberUuid);
                                if (onlineMember != null) {
                                    username = onlineMember.getName().getString();
                                } else {
                                    // 2. Fallback to the persistent profile cache matrix mapping
                                    var profile = server.getProfileCache().get(memberUuid);
                                    if (profile.isPresent()) {
                                        username = profile.get().getName();
                                    }
                                }
                            }
                            namesList.add(StringTag.valueOf(username));
                        }
                    }
                }
                rosterNbt.put(id, namesList);
            }
        }
        nbt.put("globalFactionRosterCounts", rosterNbt);

        PacketDistributor.sendToPlayer(player, new FactionSyncPayload(nbt));
    }
}
