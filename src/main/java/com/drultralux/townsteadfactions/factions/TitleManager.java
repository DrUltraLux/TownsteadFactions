package com.drultralux.townsteadfactions.factions;

import com.drultralux.townsteadfactions.integration.optional.CapitalsIntegration;
import com.drultralux.townsteadfactions.titles.TitlePreferenceManager;
import net.conczin.mca.entity.VillagerEntityMCA;
import net.conczin.mca.registry.ProfessionsMCA;
import java.util.UUID;

/**
 * Resolves the display title for a UUID (player or NPC): a self-assigned
 * cosmetic choice takes priority, then MCA Capitals' resolved nobility
 * rank (if installed and applicable), then the faction's own base-tier
 * fallback title.
 */
public class TitleManager {

    /**
     * Resolves the display title for a UUID (player or NPC): a genuine
     * earned MCA Capitals nobility rank (Knight or above) takes priority if
     * one exists, then a self-assigned cosmetic choice, then the faction's
     * own base-tier fallback title. Real, earned status always outranks a
     * cosmetic preference — this deliberately doesn't cover Leader status,
     * which is a faction-scoped concept this class has no knowledge of;
     * callers that need "Leader overrides everything" apply that check
     * themselves before falling back to this method.
     */
    public static String getResolvedTitleName(UUID entityUUID, FactionTitle fallbackLocalTitle) {
        if (CapitalsIntegration.isIntegrationFunctional()) {
            FactionTitle capitalsTitle = CapitalsIntegration.resolveTitle(entityUUID);
            if (capitalsTitle != null && capitalsTitle != FactionTitle.COMMONER) {
                return capitalsTitle.getDisplayName();
            }
        }

        FactionTitle selfAssigned = TitlePreferenceManager.getSelfAssignedTitle(entityUUID);
        if (selfAssigned != null) {
            return selfAssigned.getDisplayName();
        }

        if (CapitalsIntegration.isIntegrationFunctional()) {
            return FactionTitle.COMMONER.getDisplayName();
        }

        return fallbackLocalTitle.getDisplayName();
    }

    /**
     * Resolves the display title for an MCA villager: an earned Capitals
     * nobility rank takes priority if one exists and outranks Commoner;
     * otherwise, Guard/Archer profession maps to a military title
     * (Archers are always Soldiers; Guards are Knights if Capitals is
     * present, otherwise Soldiers); otherwise, falls back to Commoner
     * (Capitals present) or Villager (no Capitals).
     *
     * @param villager the villager to resolve a title for
     * @return the resolved display title
     */
    public static String getResolvedVillagerTitle(VillagerEntityMCA villager) {
        UUID villagerUUID = villager.getUUID();
        boolean capitalsPresent = CapitalsIntegration.isModPresent();

        if (capitalsPresent) {
            FactionTitle capitalsTitle = CapitalsIntegration.resolveTitle(villagerUUID);
            if (capitalsTitle != null && capitalsTitle != FactionTitle.COMMONER) {
                return capitalsTitle.getDisplayName();
            }
        }

        var profession = villager.getVillagerData().getProfession();
        if (profession == ProfessionsMCA.ARCHER) {
            return FactionTitle.SOLDIER.getDisplayName();
        }
        if (profession == net.conczin.mca.registry.ProfessionsMCA.GUARD) {
            return (capitalsPresent ? FactionTitle.KNIGHT : FactionTitle.SOLDIER).getDisplayName();
        }

        return (capitalsPresent ? FactionTitle.COMMONER : FactionTitle.VILLAGER).getDisplayName();
    }
}