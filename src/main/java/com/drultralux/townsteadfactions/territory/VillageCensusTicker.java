package com.drultralux.townsteadfactions.territory;

import com.aetherianartificer.townstead.villager.TownsteadVillagerState;
import com.drultralux.townsteadfactions.config.ModConfig;
import com.drultralux.townsteadfactions.factions.FactionManager;
import com.drultralux.townsteadfactions.integration.required.OriginManager;
import com.drultralux.townsteadfactions.network.FactionPacketManager;
import com.drultralux.townsteadfactions.factions.FactionTitle;
import com.drultralux.townsteadfactions.factions.TitleManager;
import com.drultralux.townsteadfactions.integration.optional.CapitalsIntegration;
import net.conczin.mca.registry.ProfessionsMCA;
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
            FactionTitle title = FactionTitle.valueOf(resolveTitleName(TitleManager.getResolvedVillagerTitle(villager)));

            String previousFactionId = FactionManager.getInstance().assignVillagerToFaction(villagerUUID, factionId, displayName, rootDisplayName, title);

            if (previousFactionId == null) {
                FactionManager.logFactionAction(factionId, displayName + " was registered as a villager of this faction.");
            } else if (!previousFactionId.equals(factionId)) {
                FactionManager.logFactionAction(factionId, displayName + " joined as a villager (formerly of " + previousFactionId + ").");
                FactionManager.logFactionAction(previousFactionId, displayName + " left to join " + factionId + ".");
            }

            populationByFaction.merge(factionId, computeVillagerWeight(villager), Integer::sum);
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
     * Computes a villager's weight toward their faction's village-control
     * score: a Noble or Monarch (real Capitals rank) is worth 5, a Guard
     * or Archer (MCA profession) is worth 2, and everyone else is worth
     * the base 1. This is the only place this weighting is applied —
     * everything downstream ({@link VillageControlManager}) just compares
     * whatever totals it's handed, with no awareness of what the numbers
     * represent.
     *
     * @param villager the villager to weigh
     * @return the villager's weight toward their faction's control score
     */
    private static int computeVillagerWeight(VillagerEntityMCA villager) {
        if (CapitalsIntegration.isIntegrationFunctional()) {
            FactionTitle rank = CapitalsIntegration.resolveTitle(villager.getUUID());
            if (rank == FactionTitle.NOBLE || rank == FactionTitle.MONARCH) {
                return 5;
            }
        }

        var profession = villager.getVillagerData().getProfession();
        if (profession == ProfessionsMCA.GUARD || profession == ProfessionsMCA.ARCHER) {
            return 2;
        }

        return 1;
    }

    /**
     * Resolves a display-title string (as produced by
     * {@link TitleManager#getResolvedVillagerTitle}) back to its matching
     * {@link FactionTitle} enum constant, since the caching layer stores
     * the enum rather than the formatted display string.
     *
     * @param displayName the resolved display title text
     * @return the matching {@link FactionTitle}, or {@link FactionTitle#VILLAGER} if none matches
     */
    private static String resolveTitleName(String displayName) {
        for (FactionTitle candidate : FactionTitle.values()) {
            if (candidate.getDisplayName().equals(displayName)) {
                return candidate.name();
            }
        }
        return FactionTitle.VILLAGER.name();
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