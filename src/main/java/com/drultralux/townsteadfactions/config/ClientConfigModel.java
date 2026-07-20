package com.drultralux.townsteadfactions.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds default configuration values for the client-side faction UI, such
 * as window size, widget positions, tab assignments, and tab ordering.
 */
public class ClientConfigModel {

    /** Interface settings keyed by setting name. */
    public final Map<String, Object> interfaceSettings = new LinkedHashMap<>();

    /**
     * Creates the client configuration model and populates it with default
     * window bounds, widget positions, tab assignments, and tab order.
     */
    public ClientConfigModel() {
        // Main window bounds
        // Main window bounds
        interfaceSettings.put("mainBoxWidth", 360);
        interfaceSettings.put("mainBoxHeight", 220);
        interfaceSettings.put("mainBoxOffsetX", 0);
        interfaceSettings.put("mainBoxOffsetY", 0);
        interfaceSettings.put("allowWindowDragging", true);
        interfaceSettings.put("dashboardScrollSpeed", 12);

        // Layout reset tracking (see TabManager for how these are used)
        interfaceSettings.put("savedLayoutVersion", 1);
        interfaceSettings.put("lastAppliedServerResetVersion", 0);

        // Default positions and tab assignments for each draggable widget.
        // Tab IDs here match TabManager.DEFAULT_TAB_* constants.
        interfaceSettings.put("treasuryWidgetX", -50);
        interfaceSettings.put("treasuryWidgetY", -30);
        interfaceSettings.put("treasuryWidgetTabId", "overview");

        interfaceSettings.put("rosterWidgetX", 40);
        interfaceSettings.put("rosterWidgetY", -10);
        interfaceSettings.put("rosterWidgetTabId", "roster");

        interfaceSettings.put("globalWidgetX", -100);
        interfaceSettings.put("globalWidgetY", 10);
        interfaceSettings.put("globalWidgetTabId", "global");

        interfaceSettings.put("avatarWidgetX", 26);
        interfaceSettings.put("avatarWidgetY", 40);
        interfaceSettings.put("avatarWidgetTabId", "overview");

        interfaceSettings.put("activityWidgetX", 26);
        interfaceSettings.put("activityWidgetY", 90);
        interfaceSettings.put("activityWidgetTabId", "overview");

        interfaceSettings.put("votingWidgetX", 250);
        interfaceSettings.put("votingWidgetY", -30);
        interfaceSettings.put("votingWidgetTabId", "overview");

        // Default tab order, as "ID;Display Name" pairs
        List<String> defaultTabOrder = new ArrayList<>();
        defaultTabOrder.add("overview;Overview");
        defaultTabOrder.add("roster;Roster");
        defaultTabOrder.add("global;Global");

        interfaceSettings.put("customizedTabOrder", defaultTabOrder);
    }
}