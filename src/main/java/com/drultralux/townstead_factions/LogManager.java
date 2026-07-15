package com.drultralux.townstead_factions;

import com.drultralux.townstead_factions.config.ModConfig;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class LogManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Shunts standard informative trace logs straight to the console/file IF debug logging is toggled on.
    public static void debug(String message, Object... params) {
        if (ModConfig.DEBUG_LOG_ENABLED) {
            LOGGER.info("[Townstead Factions DEBUG] " + message, params);
        }
    }

    // System critical lifecycle operational markers that should ALWAYS print regardless of configuration states.
    public static void info(String message, Object... params) {
        LOGGER.info("[Townstead Factions] " + message, params);
    }

    // Always capture environmental warnings so they are tracked safely inside crash logs.
    public static void warn(String message, Object... params) {
        LOGGER.warn("[Townstead Factions WARNING] " + message, params);
    }

    // Always capture code failures and tracking exceptions.
    public static void error(String message, Throwable throwable) {
        LOGGER.error("[Townstead Factions CRITICAL ERROR] " + message, throwable);
    }
}