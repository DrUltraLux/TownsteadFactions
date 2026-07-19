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
     * The most recently observed root ID for each online player this
     * session, used to detect when a player changes their origin via the
     * editor. Cleared per-player on logout.
     */
    private static final Map<UUID, String> lastKnownRootIds = new HashMap<>();

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
     * faction configured to accept that origin, if one is found. Also
     * seeds this player's cached root ID so later origin-change checks
     * have a baseline to compare against.
     *
     * @param player the player who just logged in
     */
    public static void fetchInitialRootID(ServerPlayer player) {
        try {
            String playerRootId = PlayerRoot.getRootId(player);
            String cleanedRootId = (playerRootId != null) ? playerRootId.trim() : "";
            lastKnownRootIds.put(player.getUUID(), cleanedRootId);

            if (cleanedRootId.isEmpty()) {
                LogManager.debug("Player " + player.getName().getString() + " has no assigned Townstead origin (base/default form). Using fallback faction.");
                assignFallbackFaction(player);
                return;
            }

            assignFactionForRoot(player, cleanedRootId);
        } catch (Exception e) {
            LogManager.error("Failed to safely process login faction validation metrics for player: " + player.getName().getString(), e);
        }
    }

    /**
     * Checks whether a player's current root ID differs from their last
     * known value, and if so, reassigns their faction accordingly. Intended
     * to be called periodically (see {@code FactionServerEvents}'s token-based
     * tick check) rather than every tick for every player.
     *
     * @param player the player to check
     * @return {@code true} if a change was detected and reassignment ran
     */
    public static boolean recheckPlayerOrigin(ServerPlayer player) {
        try {
            UUID playerUUID = player.getUUID();
            String currentRootId = PlayerRoot.getRootId(player);
            String cleaned = (currentRootId != null) ? currentRootId.trim() : "";
            String previous = lastKnownRootIds.put(playerUUID, cleaned);

            if (cleaned.equals(previous)) {
                return false;
            }

            if (cleaned.isEmpty()) {
                LogManager.debug("Player " + player.getName().getString() + " no longer has an assigned Townstead origin (base/default form). Using fallback faction.");
                assignFallbackFaction(player);
                return true;
            }

            LogManager.info("Detected origin change for player " + player.getName().getString() + " (now: " + getCleanName(cleaned) + ")");
            assignFactionForRoot(player, cleaned);
            return true;
        } catch (Exception e) {
            LogManager.error("Failed to recheck origin for player: " + player.getName().getString(), e);
            return false;
        }
    }

    /**
     * Removes a player's cached root ID, called on logout so the cache
     * doesn't grow unbounded over a long-running server's lifetime.
     *
     * @param playerUUID the UUID of the player who logged out
     */
    public static void clearCachedRoot(UUID playerUUID) {
        lastKnownRootIds.remove(playerUUID);
    }

    /**
     * Resolves the faction ID that a root ID is explicitly assigned to in
     * {@code factions.json}, if any.
     *
     * @param cleanedRootId the trimmed root ID to resolve
     * @return the matching faction ID, or {@code null} if none is configured
     */
    private static String resolveConfiguredFaction(String cleanedRootId) {
        for (Map.Entry<String, List<String>> entry : ModConfig.FACTIONS.getFactionsMap().entrySet()) {
            List<String> allowedOrigins = entry.getValue();
            if (allowedOrigins != null && allowedOrigins.contains(cleanedRootId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Resolves the faction a root ID should belong to: an explicit
     * {@code factions.json} assignment if one exists, otherwise the
     * fallback faction (first configured faction, by declaration order).
     * Entity-agnostic — used by both player and villager assignment paths.
     *
     * @param cleanedRootId the trimmed root ID to resolve; an empty string
     *                       is treated as having no explicit assignment
     * @return the resolved faction ID, or {@code null} if no faction is
     *         configured at all (nothing to fall back to)
     */
    public static String resolveFactionForRootId(String cleanedRootId) {
        String explicit = (cleanedRootId != null && !cleanedRootId.isEmpty()) ? resolveConfiguredFaction(cleanedRootId) : null;
        return (explicit != null) ? explicit : FactionManager.getFallbackFactionId();
    }

    /**
     * Assigns a player to the fallback faction (the first configured
     * faction, by config order), used when a player has no usable origin
     * to match against factions.json — either no origin at all, or one
     * that isn't assigned to any faction.
     *
     * @param player the player to assign
     */
    private static void assignFallbackFaction(ServerPlayer player) {
        String fallbackFactionId = FactionManager.getFallbackFactionId();
        if (fallbackFactionId == null) {
            LogManager.error("Player " + player.getName().getString() + " has no usable origin, and no fallback faction exists because no factions are currently configured. This player cannot be assigned to a faction.");
            return;
        }
        boolean changed = FactionManager.getInstance().assignPlayerToFaction(player.getUUID(), fallbackFactionId);
        if (changed) {
            FactionManager.logFactionAction(fallbackFactionId, player.getName().getString() + " joined the faction.");
        }
    }

    /**
     * Finds and assigns the faction configured to accept the given root ID,
     * if one exists.
     *
     * @param player the player to assign
     * @param cleanedRootId the player's current, trimmed root ID
     */
    private static void assignFactionForRoot(ServerPlayer player, String cleanedRootId) {
        String targetFactionKey = resolveConfiguredFaction(cleanedRootId);

        if (targetFactionKey == null) {
            String fallbackFactionId = FactionManager.getFallbackFactionId();
            if (fallbackFactionId == null) {
                LogManager.error("Player " + player.getName().getString() + " has origin '" + getCleanName(cleanedRootId) +
                        "' (" + cleanedRootId + "), which is not assigned to any faction, and no fallback faction " +
                        "exists because no factions are currently configured. This player cannot be assigned to a faction.");
                return;
            }
            LogManager.warn("Player " + player.getName().getString() + " has origin '" + getCleanName(cleanedRootId) +
                    "' (" + cleanedRootId + "), which is not assigned to any faction in factions.json. " +
                    "Falling back to faction '" + fallbackFactionId + "'. Update factions.json to assign this origin explicitly.");
            targetFactionKey = fallbackFactionId;
        }

        LogManager.info("Mapping player " + player.getName().getString() + " to faction: " + targetFactionKey + " (Origin: " + getCleanName(cleanedRootId) + ")");
        boolean changed = FactionManager.getInstance().assignPlayerToFaction(player.getUUID(), targetFactionKey);
        if (changed) {
            FactionManager.logFactionAction(targetFactionKey, player.getName().getString() + " joined the faction.");
        }
    }

    /**
     * Resolves a display-ready origin name for a player, for use in
     * roster displays. Prefers the player's live root if they're online;
     * falls back to the last known root cached this session (see the
     * token-ring origin recheck) if they're offline.
     *
     * @param playerUUID the player's UUID
     * @param onlinePlayer the player's live {@code ServerPlayer} if
     *                      currently online, or {@code null} if offline
     * @return the player's clean origin display name, or {@code "Unknown"}
     *         if no root could be resolved either way
     */
    public static String getDisplayRootName(UUID playerUUID, ServerPlayer onlinePlayer) {
        String rootId = null;
        if (onlinePlayer != null) {
            rootId = PlayerRoot.getRootId(onlinePlayer);
        }
        if (rootId == null || rootId.isEmpty()) {
            rootId = lastKnownRootIds.get(playerUUID);
        }
        if (rootId == null || rootId.isEmpty()) {
            return "Unknown";
        }
        return getCleanName(rootId);
    }
}