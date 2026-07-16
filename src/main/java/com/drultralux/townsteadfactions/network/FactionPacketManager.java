package com.drultralux.townsteadfactions.network;

import com.drultralux.townsteadfactions.LogManager;
import com.drultralux.townsteadfactions.client.FactionSyncPayload;
import com.drultralux.townsteadfactions.factions.Faction;
import com.drultralux.townsteadfactions.factions.FactionManager;
import com.drultralux.townsteadfactions.factions.MemberProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;

/**
 * Centrally coordinates server-to-client network data packing, byte stream processing,
 * and deployment routing mechanics across connected player connection channels.
 */
public class FactionPacketManager {

    /**
     * Compiles an exhaustive server-side data snapshot of active factions and roster profiles,
     * serializes it into a high-capacity compressed NBT matrix, and dispatches it straight to the client.
     *
     * @param player the specific target ServerPlayer client connection to synchronize data with
     */
    public static void sendFactionSyncPacket(ServerPlayer player) {
        if (player == null) return;

        LogManager.debug("Compiling unified high-capacity NBT sync packet for player: " + player.getName().getString());

        CompoundTag masterRootTag = new CompoundTag();

        // 1. Inject the tracking pointer indicating which faction the receiving player belongs to
        String playerFactionId = FactionManager.getInstance().getPlayerFactionId(player.getUUID());
        masterRootTag.putString("AssignedPlayerFaction", playerFactionId != null ? playerFactionId : "");

        // 2. Build the structural array tracking every single registered faction system state
        ListTag factionsListTag = new ListTag();
        Map<String, Faction> runningFactions = FactionManager.getInstance().getActiveFactions();

        for (Map.Entry<String, Faction> entry : runningFactions.entrySet()) {
            Faction faction = entry.getValue();
            CompoundTag factionTag = new CompoundTag();

            factionTag.putString("FactionID", faction.getFactionID());
            factionTag.putString("DisplayName", faction.getDisplayName());
            factionTag.putUUID("LeaderUUID", faction.getLeaderUUID());

            // Economy metrics matching dynamic integration fallbacks
            factionTag.putInt("CogsBalance", faction.getCogs());
            factionTag.putInt("FoodBalance", faction.getFood());
            factionTag.putInt("ManaBalance", faction.getMana());

            // 3. Serialize the deep social layout rosters nested inside this faction entity instance
            ListTag rosterListTag = new ListTag();
            for (Map.Entry<UUID, MemberProfile> memberEntry : faction.getMemberRoster().entrySet()) {
                MemberProfile profile = memberEntry.getValue();
                CompoundTag profileTag = new CompoundTag();

                profileTag.putUUID("MemberUUID", profile.getPlayerUUID());
                profileTag.putString("TitleEnumName", profile.getTitle().name());
                profileTag.putLong("JoinTime", profile.getJoinTimestamp());

                rosterListTag.add(profileTag);
            }
            factionTag.put("SocialRoster", rosterListTag);
            factionsListTag.add(factionTag);
        }
        masterRootTag.put("RegisteredFactionsMatrix", factionsListTag);

        // 4. Wrap inside our customized safe data payload structure and dispatch down the pipe
        try {
            FactionSyncPayload syncPayload = new FactionSyncPayload(masterRootTag);
            PacketDistributor.sendToPlayer(player, syncPayload);
            LogManager.debug("Successfully piped compressed VarInt packet matrix stream down the data channel.");
        } catch (Exception e) {
            LogManager.error("Packet stream processing sequence encountered a transport layer crash!", e);
        }
    }
}