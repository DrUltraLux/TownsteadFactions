package com.drultralux.townstead_factions.factions;

import com.drultralux.townstead_factions.integration.CapitalsIntegration;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class TitleManager {

    /**
     * Resolves the dynamic title string for any valid entity based on presence of MCA Capitals.
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