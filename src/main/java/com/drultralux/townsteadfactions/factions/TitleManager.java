package com.drultralux.townsteadfactions.factions;

import com.drultralux.townsteadfactions.integration.optional.CapitalsIntegration;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Resolves the display title shown for an entity, taking into account
 * whether the MCA Capitals mod is present and, if so, that entity's
 * Capitals-assigned rank.
 */
public class TitleManager {

    /**
     * Resolves the display title for an entity. If MCA Capitals is not
     * loaded, the faction's own fallback title is used unchanged.
     *
     * @param entity the entity to resolve a title for
     * @param fallbackLocalTitle the faction title to fall back to when
     *                            Capitals integration doesn't apply
     * @return the resolved display title
     */
    public static String getResolvedTitleName(Entity entity, FactionTitle fallbackLocalTitle) {
        if (!CapitalsIntegration.isModPresent()) {
            return fallbackLocalTitle.getDisplayName();
        }

        if (entity instanceof ServerPlayer player) {
            return resolvePlayerCapitalsTitle(player, fallbackLocalTitle);
        }

        return resolveNpcCapitalsTitle(entity, fallbackLocalTitle);
    }

    /**
     * Resolves the Capitals-aware display title for a player.
     *
     * @param player the player to resolve a title for
     * @param fallbackLocalTitle the faction title to fall back to
     * @return the resolved display title
     */
    private static String resolvePlayerCapitalsTitle(ServerPlayer player, FactionTitle fallbackLocalTitle) {
        if (CapitalsIntegration.isPlayerMonarch(player)) {
            return FactionTitle.MONARCH.getDisplayName();
        }

        if (CapitalsIntegration.isEntityNoble(player)) {
            return FactionTitle.NOBLE.getDisplayName();
        }

        if (fallbackLocalTitle == FactionTitle.LEADER || fallbackLocalTitle == FactionTitle.SOLDIER) {
            return FactionTitle.KNIGHT.getDisplayName();
        }

        return FactionTitle.COMMONER.getDisplayName();
    }

    /**
     * Resolves the Capitals-aware display title for a non-player entity.
     *
     * @param npc the entity to resolve a title for
     * @param fallbackLocalTitle the faction title to fall back to
     * @return the resolved display title
     */
    private static String resolveNpcCapitalsTitle(Entity npc, FactionTitle fallbackLocalTitle) {
        if (CapitalsIntegration.isEntityNoble(npc)) {
            return FactionTitle.NOBLE.getDisplayName();
        }

        if (fallbackLocalTitle == FactionTitle.SOLDIER) {
            if (CapitalsIntegration.isGuardNpc(npc)) {
                return FactionTitle.KNIGHT.getDisplayName();
            }
            return FactionTitle.SOLDIER.getDisplayName(); // Archer fallback target
        }

        return FactionTitle.COMMONER.getDisplayName();
    }
}