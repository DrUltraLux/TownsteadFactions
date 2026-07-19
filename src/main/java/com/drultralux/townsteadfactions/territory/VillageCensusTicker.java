package com.drultralux.townsteadfactions.territory;

import com.aetherianartificer.townstead.villager.TownsteadVillagerState;
import com.drultralux.townsteadfactions.config.ModConfig;
import com.drultralux.townsteadfactions.factions.TitleManager;
import com.drultralux.townsteadfactions.integration.required.OriginManager;
import com.drultralux.townsteadfactions.network.FactionPacketManager;
import com.drultralux.townsteadfactions.factions.FactionManager;
import com.drultralux.townsteadfactions.utils.LogManager;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.server.world.data.Village;
import net.conczin.mca.server.world.data.VillageManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Periodically sweeps one village at a time, token-ring style, to keep
 * villager faction assignments accurate and recompute which faction
 * controls each village. Runs at a config-driven real-world interval,
 * checking exactly one village per interval regardless of how many
 * villages exist, so per-tick cost never scales with world size.
 */
public class VillageCensusTicker {

    /** Server ticks elapsed since the last census check. */
    private static int ticksSinceLastCheck = 0;

    /** Index of the next village, within the current full village listing, to receive the token. */
    private static int censusTokenIndex = 0;

    /**
     * Checks whether it's time for the next census step and, if so,
     * advances the token to the next village and processes it.
     *
     * @param event the server tick event
     */
    public static void onServerTick(ServerTickEvent.Post event) {
        int intervalSeconds = ModConfig.COMMON.getInteger("villageCensusIntervalSeconds", 120);
        int intervalTicks = Math.max(1, intervalSeconds * 20);

        ticksSinceLastCheck++;
        if (ticksSinceLastCheck < intervalTicks) return;
        ticksSinceLastCheck = 0;

        MinecraftServer server = event.getServer();
        List<VillageEntry> allVillages = collectAllVillages(server);
        if (allVillages.isEmpty()) return;

        if (censusTokenIndex >= allVillages.size()) {
            censusTokenIndex = 0;
        }

        VillageEntry entry = allVillages.get(censusTokenIndex);
        censusTokenIndex = (censusTokenIndex + 1) % allVillages.size();

        processVillage(entry);
    }

    /**
     * Collects every currently known village across every loaded dimension.
     *
     * @param server the server to scan
     * @return every known village, paired with its level and composite key
     */
    private static List<VillageEntry> collectAllVillages(MinecraftServer server) {
        List<VillageEntry> result = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            VillageManager manager = VillageManager.get(level);
            for (Village village : manager) {
                if (village.getPopulation() <= 0) continue; // dead villages are just land, not tracked
                String key = level.dimension().location() + ":" + village.getId();
                result.add(new VillageEntry(level, village, key));
            }
        }
        result.sort(Comparator.comparing(VillageEntry::key));
        return result;
    }

    /**
     * Processes a single village: re-verifies/assigns each resolvable
     * resident's faction, tallies population by faction, and updates the
     * village's control state.
     *
     * @param entry the village to process
     */
    private static void processVillage(VillageEntry entry) {
        Map<String, Integer> populationByFaction = new HashMap<>();

        for (VillagerEntityMCA villager : entry.village.getResidents(entry.level)) {
            UUID villagerUUID = villager.getUUID();

            String rootId = TownsteadVillagerState.snapshotRootId(villager);
            if (rootId == null) rootId = "";

            String factionId = OriginManager.resolveFactionForRootId(rootId);
            if (factionId == null) {
                continue; // no factions configured at all — nothing to assign, safely skipped
            }

            String displayName = villager.getName().getString();
            String rootDisplayName = OriginManager.getCleanName(rootId);
            String title = TitleManager.getResolvedVillagerTitle(villager);

            VillagerFactionSavedData.VillagerRecord previousRecord = VillagerFactionRegistry.getRecord(villagerUUID);
            VillagerFactionRegistry.assignVillager(villagerUUID,
                    new VillagerFactionSavedData.VillagerRecord(factionId, displayName, rootDisplayName, title));

            if (previousRecord == null) {
                FactionManager.logFactionAction(factionId, displayName + " was registered as a villager of this faction.");
            } else if (!previousRecord.factionId().equals(factionId)) {
                FactionManager.logFactionAction(factionId, displayName + " joined as a villager (formerly of " + previousRecord.factionId() + ").");
                FactionManager.logFactionAction(previousRecord.factionId(), displayName + " left to join " + factionId + ".");
            }

            populationByFaction.merge(factionId, 1, Integer::sum);
        }

        if (!populationByFaction.isEmpty()) {
            VillageControlManager.updateVillageControl(entry.key, populationByFaction);
        }

        MinecraftServer server = entry.level.getServer();
        for (String factionId : populationByFaction.keySet()) {
            FactionPacketManager.broadcastFactionDelta(factionId, server);
        }

        LogManager.debug("Census: processed village '" + entry.key + "' (" + entry.village.getPopulation() + " residents).");
    }

    /**
     * A village paired with its owning level and composite tracking key.
     *
     * @param level the level the village belongs to
     * @param village the village itself
     * @param key the composite {@code "dimensionId:villageIntId"} key
     */
    private record VillageEntry(ServerLevel level, Village village, String key) {}
}
