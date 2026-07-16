package com.drultralux.townsteadfactions.roots;

import com.aetherianartificer.townstead.root.PlayerRoot;
import com.aetherianartificer.townstead.root.RootRegistry;
import com.drultralux.townsteadfactions.LogManager;
import com.drultralux.townsteadfactions.config.ModConfig;
import com.drultralux.townsteadfactions.factions.FactionManager;
import com.drultralux.townsteadfactions.network.FactionPacketManager;
import net.minecraft.server.level.ServerPlayer;

/**
 * Acts as a connection bridge between Townstead Root tracking layers and the Factions core.
 * Evaluates origin choices at runtime to automate dynamic faction profile sorting maps.
 */
public class OriginManager {

    /**
     * Reserved initialization method for processing data safety rails against Townstead registries.
     */
    public static void inheritTownsteadRegistries() {
        LogManager.info("Hooking into Townstead tracking registries...");
    }

    /**
     * Resolves a readable display name string out of a raw, unique Townstead resource path identity string.
     *
     * @param rootID the target unique character origin tracking ID string
     * @return a clean, human-readable display name text string
     */
    public static String getCleanNameForRoot(String rootID) {
        if (rootID == null || rootID.trim().isEmpty()) return "Unknown";

        return RootRegistry.all().stream()
                .filter(root -> root != null && root.id() != null &&
                        (root.id().getPath().equals(rootID) || root.id().toString().equals(rootID)))
                .map(root -> root.displayName().getString())
                .findFirst()
                .orElse("Invalid");
    }

    /**
     * Fetches a connecting player's active choice assignment key straight out of Townstead capability providers.
     *
     * @param player the specific target ServerPlayer client tracking context
     */
    public static void fetchInitialRootID(ServerPlayer player) {
        if (player == null) return;

        try {
            // Read tracking strings directly out of the Townstead mod's internal components
            String activeRootPathId = PlayerRoot.getRootId(player);

            if (activeRootPathId != null && !activeRootPathId.trim().isEmpty()) {
                LogManager.debug("Inherited Townstead selection index data row for player "
                        + player.getScoreboardName() + ": " + getCleanNameForRoot(activeRootPathId));

                playerFactionChecker(player, activeRootPathId.trim());
            } else {
                LogManager.debug("Player " + player.getScoreboardName() + " has not completed initialization or selected an origin root yet.");

                // Clear any leftover memory tracking maps from previous server states
                FactionManager.getInstance().assignPlayerToFaction(player.getUUID(), null);
                FactionPacketManager.sendFactionSyncPacket(player);
            }
        } catch (Exception e) {
            LogManager.error("Encountered critical tracking failure while parsing data records out of the Townstead layer!", e);
        }
    }

    /**
     * Matches the verified character root against your TOML configurations to resolve group allocations.
     */
    private static void playerFactionChecker(ServerPlayer player, String origin) {
        LogManager.debug("Processing configuration check for origin tracking label: " + origin);

        // Pull target routing from the configuration mapping files
        // (This expects a lookup method or map parameter matching your ModConfig layout)
        String assignedFactionId = ModConfig.FACTIONS.factionRegistryList.get().stream()
                .filter(id -> id != null && id.equalsIgnoreCase(origin))
                .findFirst()
                .orElse(null);

        // Fall back to clean configuration evaluations if direct mapping rules require it
        if (assignedFactionId == null) {
            assignedFactionId = origin; // Defaults directly to matching the root name path if list values align
        }

        // Apply membership adjustments directly to the centralized data caching layers
        FactionManager.getInstance().assignPlayerToFaction(player.getUUID(), assignedFactionId);

        // Immediately pipe updated network states down to the client viewports
        FactionPacketManager.sendFactionSyncPacket(player);
    }
}