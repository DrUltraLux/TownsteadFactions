package com.drultralux.townsteadfactions.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Houses pre-defined hard drive defaults and mapping parameters for common technical operations.
 * Preserves the tracking sequence markers of logging profiles and runtime variables.
 */
public class CommonConfigModel {
    /** Technical registry data map storing configuration nodes mapped to tracking values. */
    public final Map<String, Object> technicalSettings = new LinkedHashMap<>();

    /**
     * Initializes default background parameters and runtime technical debugging flags.
     */
    public CommonConfigModel() {
        // Core System Trace Variables
        technicalSettings.put("enableDebugLogging", false);
        technicalSettings.put("maxDataSyncInterval", 20);
    }
}
