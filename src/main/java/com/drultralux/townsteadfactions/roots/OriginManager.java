package com.drultralux.townsteadfactions.roots;

import com.aetherianartificer.townstead.root.PlayerRoot;
import com.aetherianartificer.townstead.root.RootRegistry;
import com.drultralux.townsteadfactions.LogManager;
import com.drultralux.townsteadfactions.config.ModConfig;
import com.drultralux.townsteadfactions.factions.FactionManager;
import com.drultralux.townsteadfactions.network.FactionPacketManager;
import net.minecraft.server.level.ServerPlayer;
import java.util.List;

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
    public static void fetchInitialRootID(net.minecraft.server.level.ServerPlayer player) {
        try {
            // Extracts the player's active origin string namespace handle natively
            //String origin = getPlayerOrigin(player);
            String origin = "";
            if (origin == null || origin.isEmpty()) {
                LogManager.debug("Player " + player.getName().getString() + " possesses an unassigned or empty origin context.");
                return;
            }

            // 💡 THE CURE: Loops through our agnostic config map to see if the player's origin matches any registered faction array rows
            String assignedFactionId = null;
            for (java.util.Map.Entry<String, net.neoforged.neoforge.common.ModConfigSpec.ConfigValue<?>> entry : com.drultralux.townsteadfactions.config.ModConfig.FACTIONS.valuesRegistry.entrySet()) {
                if (entry.getValue().get() instanceof List<?> originsList) {
                    for (Object element : originsList) {
                        if (element instanceof String registeredOrigin && registeredOrigin.equalsIgnoreCase(origin)) {
                            // The config key (e.g., "Mages", "Arcanists") becomes the live assigned faction namespace!
                            assignedFactionId = entry.getKey();
                            break;
                        }
                    }
                }
                if (assignedFactionId != null) break;
            }

            if (assignedFactionId != null) {
                LogManager.info("Mapping player profile " + player.getName().getString() + " over to the registered faction tier: " + assignedFactionId);
                com.drultralux.townsteadfactions.factions.FactionManager.getInstance().assignPlayerToFaction(player.getUUID(), assignedFactionId);
            }
        } catch (Exception e) {
            LogManager.error("Failed to safely evaluate initial login assignment matrices for user profile: " + player.getName().getString(), e);
        }
    }

    /**
     * Matches the verified character root against your TOML configurations to resolve group allocations.
     */
    private static void playerFactionChecker(ServerPlayer player, String origin) {
        LogManager.debug("Processing configuration check for origin tracking label: " + origin);

        // Pull target routing from the configuration mapping files
        // (This expects a lookup method or map parameter matching your ModConfig layout)
        List<String> registeredOrigins = ModConfig.FACTIONS.getFactionRegistryList();


        // Fall back to clean configuration evaluations if direct mapping rules require it
       // if (assignedFactionId == null) {
        //    assignedFactionId = origin; // Defaults directly to matching the root name path if list values align
       // }

        // Apply membership adjustments directly to the centralized data caching layers
       // FactionManager.getInstance().assignPlayerToFaction(player.getUUID(), assignedFactionId);

        // Immediately pipe updated network states down to the client viewports
        FactionPacketManager.sendFactionSyncPacket(player);
    }
}