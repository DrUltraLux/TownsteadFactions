package com.drultralux.townstead_factions.integration;

import net.neoforged.fml.ModList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import java.lang.reflect.Method;

public class CapitalsIntegration {
    private static final String MOD_ID = "mca_capitals";
    private static Boolean isLoaded = null;

    // Cached Reflection Targets
    private static Class<?> capitalsApiClass = null;
    private static Method isMonarchMethod = null;
    private static Method isNobleMethod = null;
    private static Method isGuardMethod = null;
    private static boolean reflectionInitialized = false;

    public static boolean isModPresent() {
        if (isLoaded == null) {
            isLoaded = ModList.get().isLoaded(MOD_ID);
        }
        return isLoaded;
    }

    private static void initReflection() {
        if (reflectionInitialized) return;
        reflectionInitialized = true;
        if (!isModPresent()) return;

        try {
            // Target the internal MCA Capitals API or data lookup class
            capitalsApiClass = Class.forName("com.mca.capitals.common.api.CapitalsAPI"); // Adjust string if actual package path differs

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
