package com.drultralux.townsteadfactions.roots;

import com.drultralux.townsteadfactions.LogManager;
import com.drultralux.townsteadfactions.config.ModConfig;
import com.aetherianartificer.townstead.root.RootRegistry;
import com.aetherianartificer.townstead.root.PlayerRoot;
import com.aetherianartificer.townstead.root.Root;
import net.minecraft.server.level.ServerPlayer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Directly synchronizes with Townstead registries to cache rootID and cleanID mappings.
 * Serves validation and player origin data directly up to the FactionManager.
 */
public class OriginManager {
    /**
     * Mirror cache of Townstead's master map loaded from datapacks.
     * Key: rootID (e.g., "townstead_classic:high_elf") -> Value: cleanID (e.g., "High Elf")
     */
    private static final Map<String, String> originsCache = new HashMap<>();

    /**
     * Pulls the complete list of active origins loaded into memory directly from Townstead.
     * Maps native properties safely without relying on raw string formatting loops.
     */
    public static void initializeFromTownstead() {
        originsCache.clear();
        try {
            // Fetch the raw list directly from Townstead's core API registry
            List<Root> townsteadList = RootRegistry.all();

            if (townsteadList != null) {
                for (Root root : townsteadList) {
                    if (root == null) continue;

                    String rootId = root.id().toString();
                    String cleanId = root.displayName().getString();

                    if (rootId != null && cleanId != null) {
                        originsCache.put(rootId.trim(), cleanId.trim());
                    }
                }
                LogManager.info("Successfully mirrored Townstead origin objects. Active options cached: " + originsCache.size());
            } else {
                LogManager.warn("Townstead RootRegistry.all() returned a null list collection!");
            }
        } catch (Exception e) {
            LogManager.error("Failed to parse master origin elements out of Townstead registries!", e);
        }
    }

    /**
     * Verifies whether a given identifier represents a true, active origin loaded by Townstead.
     * Used by FactionManager to prevent the creation of dead or unmapped factions.
     *
     * @param rootId The unique structural identifier string
     * @return true if the rootID exists inside Townstead's registries, false otherwise
     */
    public static boolean isValidOrigin(String rootId) {
        if (rootId == null) return false;
        return originsCache.containsKey(rootId.trim());
    }

    /**
     * Fetches the clean English display title corresponding to a given rootID string.
     * Used by UI screens, overlays, and chat formatting modules.
     *
     * @param rootId The structural identifier string
     * @return the clean English display string title (e.g., "High Elf")
     */
    public static String getCleanName(String rootId) {
        if (rootId == null) return "Unknown";
        return originsCache.getOrDefault(rootId.trim(), rootId);
    }

    /**
     * Direct getter to expose our tracked origins map registry.
     */
    public static Map<String, String> getOriginsCache() {
        return originsCache;
    }

    /**
     * Examines a joining player's selected character profile to resolve their target faction membership.
     * Fully trusts Townstead's pre-validated rootId data and maps it straight to factions.json properties.
     *
     * @param player The target server player logging in
     */
    public static void fetchInitialRootID(ServerPlayer player) {
        try {
            UUID playerUUID = player.getUUID();

            // Directly extract the pre-validated string ID from Townstead's player capabilities
            String playerRootId = PlayerRoot.getRootId(player);

            if (playerRootId == null || playerRootId.trim().isEmpty()) {
                LogManager.debug("Player " + player.getName().getString() + " does not possess an assigned rootId configuration.");
                return;
            }

            String cleanedRootId = playerRootId.trim();
            String targetFactionKey = null;

            // Scan through factions.json to see which category array contains this specific player rootId
            for (Map.Entry<String, List<String>> entry : ModConfig.FACTIONS.getFactionsMap().entrySet()) {
                List<String> allowedOrigins = entry.getValue();
                if (allowedOrigins != null && allowedOrigins.contains(cleanedRootId)) {
                    targetFactionKey = entry.getKey();
                    break;
                }
            }

            if (targetFactionKey != null) {
                LogManager.info("Login Match: Mapping player " + player.getName().getString() + " to faction: " + targetFactionKey + " (Origin: " + getCleanName(cleanedRootId) + ")");
                com.drultralux.townsteadfactions.factions.FactionManager.getInstance().assignPlayerToFaction(playerUUID, targetFactionKey);
            }
        } catch (Exception e) {
            LogManager.error("Failed to safely process login faction validation metrics for player: " + player.getName().getString(), e);
        }
    }
}