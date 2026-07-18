package com.drultralux.townsteadfactions.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds default configuration values for the client-side faction UI, such
 * as window size, widget positions, and tab ordering.
 */
public class ClientConfigModel {

    /** Interface settings keyed by setting name. */
    public final Map<String, Object> interfaceSettings = new LinkedHashMap<>();

    /**
     * Creates the client configuration model and populates it with default
     * window bounds, widget positions, and tab order.
     */
    public ClientConfigModel() {
        // Main window bounds
        interfaceSettings.put("mainBoxWidth", 360);
        interfaceSettings.put("mainBoxHeight", 220);
        interfaceSettings.put("allowWindowDragging", true);

        // Default positions and tab assignments for each draggable widget
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

        // Default tab order, as "ID;Display Name" pairs
        List<String> defaultTabOrder = new ArrayList<>();
        defaultTabOrder.add("OVERVIEW;Overview");
        defaultTabOrder.add("ROSTER;Roster");
        defaultTabOrder.add("GLOBAL;Global");

        interfaceSettings.put("customizedTabOrder", defaultTabOrder);
    }
}