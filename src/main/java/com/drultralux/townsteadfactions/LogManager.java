package com.drultralux.townsteadfactions;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Centrally manages unified logging operations across the Townstead Factions lifecycle.
 * Provides configurable debug tracing toggles to prevent console clutter while maintaining diagnostic depth.
 */
public class LogManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean debugEnabled = false;

    /**
     * Initializes the central debug logging toggle state based on configuration settings.
     *
     * @param enabled true if detailed execution trace statements should be printed to log output
     */
    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
        info("Logging system configured. Debug tracing state: " + enabled);
    }

    /**
     * Prints an informational message to the main server console and log tracking output.
     *
     * @param message the text string containing informational data to log
     */
    public static void info(String message) {
        LOGGER.info("[TownsteadFactions] {}", message);
    }

    /**
     * Prints a warning notification to indicate minor processing failures or missing non-critical resources.
     *
     * @param message the text string containing warning details to log
     */
    public static void warn(String message) {
        LOGGER.warn("[TownsteadFactions] WARNING: {}", message);
    }

    /**
     * Prints a critical error or exception signature to logs when execution pathways break completely.
     *
     * @param message the text string explaining where the exception happened
     * @param throwable the raw exception trace to print
     */
    public static void error(String message, Throwable throwable) {
        LOGGER.error("[TownsteadFactions] CRITICAL ERROR: " + message, throwable);
    }

    /**
     * Conditionally outputs high-density trace summaries for diagnostic tracing if debug mode is active.
     *
     * @param message the trace string containing step-by-step logic updates
     */
    public static void debug(String message) {
        if (debugEnabled) {
            LOGGER.info("[TownsteadFactions-Debug] {}", message);
        }
    }
}