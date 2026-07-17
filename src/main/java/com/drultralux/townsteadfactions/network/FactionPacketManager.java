package com.drultralux.townsteadfactions.network;

import com.drultralux.townsteadfactions.client.FactionSyncPayload;
import com.drultralux.townsteadfactions.factions.Faction;
import com.drultralux.townsteadfactions.factions.FactionManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
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
        UUID playerUUID = player.getUUID();

        Faction playerFaction = null;
        Map<String, Faction> serverFactionsMap = FactionManager.getInstance().getActiveFactions();

        for (Faction faction : serverFactionsMap.values()) {
            if (faction.getMembers() != null && faction.getMembers().contains(playerUUID)) {
                playerFaction = faction;
                break;
            }
        }

        String assignedFactionId = (playerFaction != null) ? playerFaction.getId() : "none";

        Map<String, Integer> globalFactionRosterCounts = new HashMap<>();
        for (Map.Entry<String, Faction> entry : serverFactionsMap.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                int count = entry.getValue().getMembers() != null ? entry.getValue().getMembers().size() : 0;
                globalFactionRosterCounts.put(entry.getKey().trim(), count);
            }
        }
        int cogs = (playerFaction != null) ? playerFaction.getCogs() : 0;
        int food = (playerFaction != null) ? playerFaction.getFood() : 0;
        int mana = (playerFaction != null) ? playerFaction.getMana() : 0;

        PacketDistributor.sendToPlayer(
                player,
                new FactionSyncPayload(assignedFactionId, cogs, food, mana, globalFactionRosterCounts)
        );
    }
}
