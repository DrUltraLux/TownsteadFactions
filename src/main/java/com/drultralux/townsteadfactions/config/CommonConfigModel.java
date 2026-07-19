package com.drultralux.townsteadfactions.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds configuration values that apply to both the client and the server,
 * such as debug logging and data synchronization settings.
 */
public class CommonConfigModel {
    /** Technical registry data map storing configuration nodes mapped to tracking values. */
    public final Map<String, Object> technicalSettings = new LinkedHashMap<>();

    /**
     * Initializes default background parameters and runtime technical debugging flags.
     */
    public CommonConfigModel() {
        technicalSettings.put("enableDebugLogging", false);
        technicalSettings.put("maxDataSyncInterval", 20);
        // Real-world seconds between village census sweeps (token-ring: one village checked per interval)
        technicalSettings.put("villageCensusIntervalSeconds", 10);
        technicalSettings.put("factionActivityLogCap", 2000);
    }
}
