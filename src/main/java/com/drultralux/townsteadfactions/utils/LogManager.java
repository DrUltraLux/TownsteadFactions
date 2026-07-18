package com.drultralux.townsteadfactions.utils;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Centralized logging utility for Townstead Factions, wrapping the mod's
 * SLF4J logger with consistent message prefixes and an optional debug mode.
 */
public class LogManager {

    /** The underlying SLF4J logger used for all log output. */
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Whether debug-level messages are currently being logged. */
    private static boolean debugEnabled = false;

    /**
     * Enables or disables debug logging.
     *
     * @param enabled {@code true} to print debug trace messages, {@code false} to suppress them
     */
    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
        info("Logging system configured. Debug tracing state: " + enabled);
    }

    /**
     * Logs an informational message.
     *
     * @param message the message to log
     */
    public static void info(String message) {
        LOGGER.info("[TownsteadFactions] {}", message);
    }

    /**
     * Logs a warning message, for recoverable issues or missing optional
     * resources.
     *
     * @param message the message to log
     */
    public static void warn(String message) {
        LOGGER.warn("[TownsteadFactions] WARNING: {}", message);
    }

    /**
     * Logs an error message along with an exception's stack trace.
     *
     * @param message a description of where the error occurred
     * @param throwable the exception to log
     */
    public static void error(String message, Throwable throwable) {
        LOGGER.error("[TownsteadFactions] CRITICAL ERROR: " + message, throwable);
    }

    /**
     * Logs a debug message, but only if debug logging is currently enabled.
     *
     * @param message the message to log
     */
    public static void debug(String message) {
        if (debugEnabled) {
            LOGGER.info("[TownsteadFactions-Debug] {}", message);
        }
    }
}