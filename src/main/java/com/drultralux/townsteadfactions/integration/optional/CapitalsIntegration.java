package com.drultralux.townsteadfactions.integration.optional;

import net.neoforged.fml.ModList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import java.lang.reflect.Method;

/**
 * Provides optional, reflection-based integration with the MCA Capitals
 * mod, allowing faction titles to reflect Capitals-assigned monarch,
 * noble, and guard status when that mod is installed.
 *
 * <p>All lookups degrade gracefully to {@code false} if MCA Capitals is
 * not present, or if its API doesn't match what this class expects.</p>
 */
public class CapitalsIntegration {

    /** The mod ID used to detect whether MCA Capitals is loaded. */
    private static final String MOD_ID = "mca_capitals";

    /** Cached result of the mod-presence check, or {@code null} if not yet checked. */
    private static Boolean isLoaded = null;

    /** The reflectively loaded Capitals API class, or {@code null} if unavailable. */
    private static Class<?> capitalsApiClass = null;

    /** Reflective handle for the Capitals "is monarch" check. */
    private static Method isMonarchMethod = null;

    /** Reflective handle for the Capitals "is noble" check. */
    private static Method isNobleMethod = null;

    /** Reflective handle for the Capitals "is guard" check. */
    private static Method isGuardMethod = null;

    /** Whether {@link #initReflection()} has already run. */
    private static boolean reflectionInitialized = false;

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
     * Resolves the Capitals API class and its methods via reflection, if
     * MCA Capitals is present. Safe to call multiple times; only runs once.
     * On any failure, all cached method handles are reset to {@code null}
     * so lookups fail closed rather than throw.
     */
    private static void initReflection() {
        if (reflectionInitialized) return;
        reflectionInitialized = true;
        if (!isModPresent()) return;

        try {
            // Target the internal MCA Capitals API or data lookup class.
            // NOTE: verify this fully-qualified name against the actual mod if lookups start failing silently.
            capitalsApiClass = Class.forName("com.mca.capitals.common.api.CapitalsAPI");

            isMonarchMethod = capitalsApiClass.getMethod("isMonarch", ServerPlayer.class);
            isNobleMethod = capitalsApiClass.getMethod("isNoble", Entity.class); // Handles both Player and NPC if unified
            isGuardMethod = capitalsApiClass.getMethod("isGuard", Entity.class);
        } catch (Exception e) {
            capitalsApiClass = null;
            isMonarchMethod = null;
            isNobleMethod = null;
            isGuardMethod = null;
        }
    }

    /**
     * Checks whether the given player is a monarch, according to MCA
     * Capitals.
     *
     * @param player the player to check
     * @return {@code true} if Capitals reports the player as a monarch;
     *         {@code false} if the mod isn't present or the check fails
     */
    public static boolean isPlayerMonarch(ServerPlayer player) {
        if (!isModPresent()) return false;
        initReflection();
        if (isMonarchMethod == null) return false;
        try {
            return (Boolean) isMonarchMethod.invoke(null, player);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks whether the given entity is a noble, according to MCA
     * Capitals.
     *
     * @param entity the entity to check
     * @return {@code true} if Capitals reports the entity as noble;
     *         {@code false} if the mod isn't present or the check fails
     */
    public static boolean isEntityNoble(Entity entity) {
        if (!isModPresent()) return false;
        initReflection();
        if (isNobleMethod == null) return false;
        try {
            return (Boolean) isNobleMethod.invoke(null, entity);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks whether the given entity is a guard, according to MCA
     * Capitals.
     *
     * @param entity the entity to check
     * @return {@code true} if Capitals reports the entity as a guard;
     *         {@code false} if the mod isn't present or the check fails
     */
    public static boolean isGuardNpc(Entity entity) {
        if (!isModPresent()) return false;
        initReflection();
        if (isGuardMethod == null) return false;
        try {
            return (Boolean) isGuardMethod.invoke(null, entity);
        } catch (Exception e) {
            return false;
        }
    }
}