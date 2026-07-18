package com.drultralux.townsteadfactions.integration.required;

import com.drultralux.townsteadfactions.utils.LogManager;
import com.drultralux.townsteadfactions.config.ModConfig;
import com.drultralux.townsteadfactions.factions.FactionManager;
import com.aetherianartificer.townstead.root.RootRegistry;
import com.aetherianartificer.townstead.root.PlayerRoot;
import com.aetherianartificer.townstead.root.Root;
import net.minecraft.server.level.ServerPlayer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mirrors origin (race) data from the Townstead mod and maps players to
 * factions based on their assigned origin.
 */
public class OriginManager {

    /**
     * A cache of Townstead's origins, mapping root ID (e.g.
     * {@code "townstead_classic:high_elf"}) to display name (e.g.
     * {@code "High Elf"}).
     */
    private static final Map<String, String> originsCache = new HashMap<>();

    /**
     * Rebuilds {@link #originsCache} from Townstead's current
     * {@link RootRegistry}. Safe to call again to refresh the cache.
     */
    public static void initializeFromTownstead() {
        originsCache.clear();
        try {
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
     * Checks whether a root ID corresponds to a currently known Townstead
     * origin. Used to prevent factions from referencing unmapped origins.
     *
     * @param rootId the root ID to check
     * @return {@code true} if the root ID exists in Townstead's registry
     */
    public static boolean isValidOrigin(String rootId) {
        if (rootId == null) return false;
        return originsCache.containsKey(rootId.trim());
    }

    /**
     * Returns the human-readable display name for a root ID.
     *
     * @param rootId the root ID to look up
     * @return the display name (e.g. {@code "High Elf"}), or the original
     *         {@code rootId} if it isn't known, or {@code "Unknown"} if
     *         {@code rootId} is {@code null}
     */
    public static String getCleanName(String rootId) {
        if (rootId == null) return "Unknown";
        return originsCache.getOrDefault(rootId.trim(), rootId);
    }

    /**
     * Returns the live origins cache.
     *
     * @return the map of root ID to display name
     */
    public static Map<String, String> getOriginsCache() {
        return originsCache;
    }

    /**
     * Resolves a logging-in player's origin and assigns them to the
     * faction configured to accept that origin, if one is found.
     *
     * @param player the player who just logged in
     */
    public static void fetchInitialRootID(ServerPlayer player) {
        try {
            UUID playerUUID = player.getUUID();

            String playerRootId = PlayerRoot.getRootId(player);

            if (playerRootId == null || playerRootId.trim().isEmpty()) {
                LogManager.debug("Player " + player.getName().getString() + " does not possess an assigned rootId configuration.");
                return;
            }

            String cleanedRootId = playerRootId.trim();
            String targetFactionKey = null;

            // Find which faction's configured origin list contains this player's root ID
            for (Map.Entry<String, List<String>> entry : ModConfig.FACTIONS.getFactionsMap().entrySet()) {
                List<String> allowedOrigins = entry.getValue();
                if (allowedOrigins != null && allowedOrigins.contains(cleanedRootId)) {
                    targetFactionKey = entry.getKey();
                    break;
                }
            }

            if (targetFactionKey != null) {
                LogManager.info("Login Match: Mapping player " + player.getName().getString() + " to faction: " + targetFactionKey + " (Origin: " + getCleanName(cleanedRootId) + ")");
                FactionManager.getInstance().assignPlayerToFaction(playerUUID, targetFactionKey);
            }
        } catch (Exception e) {
            LogManager.error("Failed to safely process login faction validation metrics for player: " + player.getName().getString(), e);
        }
    }
}