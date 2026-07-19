package com.drultralux.townsteadfactions.integration.optional;

import com.drultralux.townsteadfactions.factions.FactionTitle;
import com.drultralux.townsteadfactions.utils.LogManager;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Provides optional, reflection-based integration with the MCA Capitals
 * mod, resolving a player or NPC's in-mod nobility rank into this mod's
 * {@link FactionTitle} tiers (Monarch/Knight/Noble/Commoner) when
 * Capitals is installed.
 *
 * <p>Capitals exposes no stable, versioned public API for this data —
 * only a handful of genuinely public methods on its internal
 * {@code CapitalRecord} and {@code CapitalTitleResolver} classes. This
 * integration is coupled to those classes' current shape and may need
 * updating if a future Capitals release changes them; all lookups
 * degrade gracefully to {@code null} rather than throwing if that
 * happens.</p>
 *
 * <p>Two Capitals roles are deliberately not mapped here: Maester (its
 * resolution requires a resident-population scan, not a simple lookup)
 * and Herald (ranks below Knight in Capitals' own hierarchy, so it falls
 * through to Commoner rather than being force-fit into Noble). Both
 * fall through to {@link FactionTitle#COMMONER}.</p>
 */
public class CapitalsIntegration {

    /** The mod ID used to detect whether MCA Capitals is loaded. */
    private static final String MOD_ID = "mcacapitals";

    /** Cached result of the mod-presence check, or {@code null} if not yet checked. */
    private static Boolean isLoaded = null;

    /** Whether {@link #initReflection()} has already run. */
    private static boolean reflectionInitialized = false;

    /** Whether all reflection targets were resolved successfully. */
    private static boolean reflectionAvailable = false;

    private static Method findCapitalForEntity;
    private static Method getSovereign;
    private static Method getPlayerSovereignId;
    private static Method getConsort;
    private static Method getDowager;
    private static Method isKnight;
    private static Method isRoyalGuard;
    private static Method isDuke;
    private static Method isLord;
    private static Method isPrinceConsort;
    private static Method isDowagerPrince;
    private static Method isRoyalChild;
    private static Method isMarriageDuke;
    private static Method isDowagerDuke;
    private static Method getCommander;
    private static Method getHand;
    private static Method getGrandMaester;

    /**
     * Checks whether the MCA Capitals mod is currently loaded. The result
     * is cached after the first call.
     *
     * @return {@code true} if MCA Capitals is loaded
     */
    public static boolean isModPresent() {
        if (isLoaded == null) {
            isLoaded = ModList.get().isLoaded(MOD_ID);
        }
        return isLoaded;
    }

    /**
     * Resolves the reflection targets used by this class, if MCA Capitals
     * is present. Safe to call multiple times; only runs once. On any
     * failure, all cached method handles are reset so lookups fail closed
     * rather than throw.
     */
    private static void initReflection() {
        if (reflectionInitialized) return;
        reflectionInitialized = true;
        if (!isModPresent()) return;

        try {
            Class<?> resolverClass = Class.forName("com.majesttyx.mcacapitals.capital.CapitalTitleResolver");
            Class<?> recordClass = Class.forName("com.majesttyx.mcacapitals.capital.CapitalRecord");

            findCapitalForEntity = resolverClass.getMethod("findCapitalForEntity", UUID.class);

            getSovereign = recordClass.getMethod("getSovereign");
            getPlayerSovereignId = recordClass.getMethod("getPlayerSovereignId");
            getConsort = recordClass.getMethod("getConsort");
            getDowager = recordClass.getMethod("getDowager");
            isKnight = recordClass.getMethod("isKnight", UUID.class);
            isRoyalGuard = recordClass.getMethod("isRoyalGuard", UUID.class);
            isDuke = recordClass.getMethod("isDuke", UUID.class);
            isLord = recordClass.getMethod("isLord", UUID.class);
            isPrinceConsort = recordClass.getMethod("isPrinceConsort", UUID.class);
            isDowagerPrince = recordClass.getMethod("isDowagerPrince", UUID.class);
            isRoyalChild = recordClass.getMethod("isRoyalChild", UUID.class);
            isMarriageDuke = recordClass.getMethod("isMarriageDuke", UUID.class);
            isDowagerDuke = recordClass.getMethod("isDowagerDuke", UUID.class);
            getCommander = recordClass.getMethod("getCommander");
            getHand = recordClass.getMethod("getHand");
            getGrandMaester = recordClass.getMethod("getGrandMaester");

            reflectionAvailable = true;
        } catch (Exception e) {
            LogManager.warn("MCA Capitals is installed, but its internal class structure no longer matches what " +
                    "Townstead Factions expects (this integration may need updating for this Capitals version). " +
                    "Capitals-based title resolution is disabled until then.");
            reflectionAvailable = false;
        }
    }

    /**
     * Resolves an entity's MCA Capitals nobility rank into a
     * {@link FactionTitle} tier. Works for players and NPCs alike, since
     * Capitals tracks roles by UUID regardless of entity type.
     *
     * @param entityUUID the entity to resolve a title for
     * @return the resolved title, or {@code null} if Capitals isn't
     *         present, the entity isn't part of any capital, or
     *         resolution failed
     */
    public static FactionTitle resolveTitle(UUID entityUUID) {
        if (entityUUID == null || !isModPresent()) return null;
        initReflection();
        if (!reflectionAvailable) return null;

        try {
            Object capitalRecord = findCapitalForEntity.invoke(null, entityUUID);
            if (capitalRecord == null) return null;

            if (entityUUID.equals(getSovereign.invoke(capitalRecord))
                    || entityUUID.equals(getPlayerSovereignId.invoke(capitalRecord))
                    || entityUUID.equals(getConsort.invoke(capitalRecord))
                    || entityUUID.equals(getDowager.invoke(capitalRecord))) {
                return FactionTitle.MONARCH;
            }

            if ((boolean) isKnight.invoke(capitalRecord, entityUUID)
                    || (boolean) isRoyalGuard.invoke(capitalRecord, entityUUID)) {
                return FactionTitle.KNIGHT;
            }

            if ((boolean) isDuke.invoke(capitalRecord, entityUUID)
                    || (boolean) isLord.invoke(capitalRecord, entityUUID)
                    || (boolean) isPrinceConsort.invoke(capitalRecord, entityUUID)
                    || (boolean) isDowagerPrince.invoke(capitalRecord, entityUUID)
                    || (boolean) isRoyalChild.invoke(capitalRecord, entityUUID)
                    || (boolean) isMarriageDuke.invoke(capitalRecord, entityUUID)
                    || (boolean) isDowagerDuke.invoke(capitalRecord, entityUUID)
                    || entityUUID.equals(getCommander.invoke(capitalRecord))
                    || entityUUID.equals(getHand.invoke(capitalRecord))
                    || entityUUID.equals(getGrandMaester.invoke(capitalRecord))) {
                return FactionTitle.NOBLE;
            }

            return FactionTitle.COMMONER;
        } catch (Exception e) {
            LogManager.error("Failed to resolve MCA Capitals title for entity: " + entityUUID, e);
            return null;
        }
    }
}