package com.drultralux.townstead_factions;

import com.aetherianartificer.townstead.root.PlayerRoot;
import com.aetherianartificer.townstead.root.RootRegistry;
import com.aetherianartificer.townstead.root.Root;
import net.minecraft.server.level.ServerPlayer;

public class OriginManager {

    private static String rootID = null;

    public static void inheritTownsteadRegistries() {
        LogManager.info("[Townstead Factions] Dynamically hooked into Townstead RootRegistry Record List.");
        //not doing anything here yet. reserved for any data validation we might need later.
    }

    //Easy name cleaner for me to plop into the UI
    public static String getCleanNameForRoot(String rootID) {
        if (rootID == null) return "Unknown";

        return RootRegistry.all().stream()
                .filter((Root root) -> root != null && root.id() != null &&
                        (root.id().getPath().equals(rootID) || root.id().toString().equals(rootID)))
                .map((Root root) -> root.displayName().getString())
                .findFirst()
                .orElse("Invalid");
    }

    public static void fetchInitialRootID(ServerPlayer player)
    {
        try {
            rootID = PlayerRoot.getRootId(player);

            if (rootID != null) {
                LogManager.info("[Townstead Factions] Successfully inherited choice from Townstead for player {}: {}", player.getScoreboardName(), OriginManager.getCleanNameForRoot(rootID));
                playerFactionChecker(player, rootID);
            } else {
                LogManager.info("[Townstead Factions] Player {} has not completed character initialization or selected an origin yet.", player.getScoreboardName());
            }
        } catch (Exception e) {
            LogManager.error("[Townstead Factions Error] Encountered critical failure during data inheritance lookup: ", e);
        }
    }

    private static void playerFactionChecker(ServerPlayer player, String origin) {
        if (player == null || origin == null) return;

        String assignedFactionName = ModConfig.ROOT_TO_FACTION_MAP.getOrDefault(origin, "None");

        // Pass the resolved configuration faction name over to our dynamic FactionManager
        FactionManager.processPlayerAssignment(player, assignedFactionName);
    }
}