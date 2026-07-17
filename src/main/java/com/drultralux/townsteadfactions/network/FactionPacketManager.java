package com.drultralux.townsteadfactions.network;

import com.drultralux.townsteadfactions.client.FactionSyncPayload;
import com.drultralux.townsteadfactions.factions.Faction;
import com.drultralux.townsteadfactions.factions.FactionManager;
import net.minecraft.nbt.CompoundTag;
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

        String factionId = FactionManager.getPlayerFactionId(playerUUID);

        CompoundTag nbt = new CompoundTag();
        nbt.putString("assignedFactionId", (factionId != null) ? factionId : "none");

        nbt.putInt("cogs", FactionManager.getPlayerFactionAsset(playerUUID, "cogs"));
        nbt.putInt("food", FactionManager.getPlayerFactionAsset(playerUUID, "food"));
        nbt.putInt("mana", FactionManager.getPlayerFactionAsset(playerUUID, "mana"));

        CompoundTag rosterNbt = new CompoundTag();
        java.util.Set<String> factionIds = FactionManager.getInstance().getActiveFactions().keySet();
        for (String id : factionIds) {
            if (id != null) {
                rosterNbt.putInt(id, FactionManager.getFactionMemberCount(id));
            }
        }
        nbt.put("globalFactionRosterCounts", rosterNbt);

        PacketDistributor.sendToPlayer(player, new FactionSyncPayload(nbt));
    }
}
