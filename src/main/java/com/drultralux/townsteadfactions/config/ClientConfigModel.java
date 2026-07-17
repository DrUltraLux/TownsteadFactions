package com.drultralux.townsteadfactions.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Houses pre-defined hard drive defaults and mapping parameters for user-interface elements.
 * Preserves the visual ordering specifications of core window dimensions and tab sorting metrics.
 */
public class ClientConfigModel {
    /** Structural registry data map storing configuration nodes mapped to layout labels. */
    public final Map<String, Object> interfaceSettings = new LinkedHashMap<>();

    /**
     * Initializes default spatial coordinate markers and builds out the customized tab collection arrays.
     */
    public ClientConfigModel() {
        // Core Resizable Overlay Frame Window Bounding Metrics
        interfaceSettings.put("mainBoxWidth", 360);
        interfaceSettings.put("mainBoxHeight", 220);
        interfaceSettings.put("allowWindowDragging", true);

        // Individual Draggable Sub-Widget Coordinate Snap Maps
        interfaceSettings.put("treasuryWidgetX", -50);
        interfaceSettings.put("treasuryWidgetY", -30);
        interfaceSettings.put("treasuryWidgetTab", 0);

        interfaceSettings.put("rosterWidgetX", 40);
        interfaceSettings.put("rosterWidgetY", -10);
        interfaceSettings.put("rosterWidgetTab", 1);

        interfaceSettings.put("globalWidgetX", -100);
        interfaceSettings.put("globalWidgetY", 10);
        interfaceSettings.put("globalWidgetTab", 2);

        interfaceSettings.put("avatarWidgetX", 26);
        interfaceSettings.put("avatarWidgetY", 40);
        interfaceSettings.put("avatarWidgetTab", 0);

        interfaceSettings.put("activityWidgetX", 26);
        interfaceSettings.put("activityWidgetY", 90);
        interfaceSettings.put("activityWidgetTab", 0);

        // Translated Tab Names Selection Index Strings
        List<String> defaultTabOrder = new ArrayList<>();
        defaultTabOrder.add("OVERVIEW;Overview");
        defaultTabOrder.add("ROSTER;Roster");
        defaultTabOrder.add("GLOBAL;Global");

        interfaceSettings.put("customizedTabOrder", defaultTabOrder);
    }
}
