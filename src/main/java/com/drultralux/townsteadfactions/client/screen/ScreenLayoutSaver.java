package com.drultralux.townsteadfactions.client.screen;

import com.drultralux.townsteadfactions.client.screen.widget.TabPanelWidget;
import com.drultralux.townsteadfactions.config.ModConfig;
import com.drultralux.townsteadfactions.utils.LogManager;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles reading and writing of the faction dashboard's full layout —
 * window size, widget positions and tab assignments, and the current tab
 * list — to the client configuration file.
 */
public class ScreenLayoutSaver {

    /**
     * Saves the full current dashboard layout to the client config in one
     * pass, then writes the config to disk: every draggable widget's
     * position and assigned tab, the main window size, and the current
     * list of tabs (in case any were added, removed, renamed, or
     * reordered).
     *
     * @param tx treasury widget x position
     * @param ty treasury widget y position
     * @param tTabId treasury widget's assigned tab ID
     * @param rx roster widget x position
     * @param ry roster widget y position
     * @param rTabId roster widget's assigned tab ID
     * @param gx global widget x position
     * @param gy global widget y position
     * @param gTabId global widget's assigned tab ID
     * @param ax avatar widget x position
     * @param ay avatar widget y position
     * @param aTabId avatar widget's assigned tab ID
     * @param lx activity log widget x position
     * @param ly activity log widget y position
     * @param lTabId activity log widget's assigned tab ID
     * @param boxW main dashboard window width
     * @param boxH main dashboard window height
     * @param tabs the current tabs, in display order, to persist
     */
    public static void saveFullLayout(
            int tx, int ty, String tTabId,
            int rx, int ry, String rTabId,
            int gx, int gy, String gTabId,
            int ax, int ay, String aTabId,
            int lx, int ly, String lTabId,
            int boxW, int boxH,
            List<TabPanelWidget> tabs) {
        try {
            setAgnosticValue("treasuryWidgetX", tx);
            setAgnosticValue("treasuryWidgetY", ty);
            setAgnosticValue("treasuryWidgetTabId", tTabId);

            setAgnosticValue("rosterWidgetX", rx);
            setAgnosticValue("rosterWidgetY", ry);
            setAgnosticValue("rosterWidgetTabId", rTabId);

            setAgnosticValue("globalWidgetX", gx);
            setAgnosticValue("globalWidgetY", gy);
            setAgnosticValue("globalWidgetTabId", gTabId);

            setAgnosticValue("avatarWidgetX", ax);
            setAgnosticValue("avatarWidgetY", ay);
            setAgnosticValue("avatarWidgetTabId", aTabId);

            setAgnosticValue("activityWidgetX", lx);
            setAgnosticValue("activityWidgetY", ly);
            setAgnosticValue("activityWidgetTabId", lTabId);

            setAgnosticValue("mainBoxWidth", boxW);
            setAgnosticValue("mainBoxHeight", boxH);

            List<String> encodedTabs = new ArrayList<>();
            for (TabPanelWidget tab : tabs) {
                encodedTabs.add(tab.getId() + ";" + tab.getTitle());
            }
            setAgnosticValue("customizedTabOrder", encodedTabs);

            ModConfig.CLIENT_SPEC.save();
            LogManager.debug("Dynamic window layouts successfully committed to disk files.");
        } catch (Exception e) {
            LogManager.error("Failed to commit operational screen coordinates to configuration!", e);
        }
    }

    /**
     * Saves the layout schema version this client's saved layout was built
     * against. Used to detect when a shipped mod update requires
     * discarding old layouts (see {@code TabManager}).
     *
     * @param version the layout schema version to record
     */
    public static void saveLayoutVersion(int version) {
        try {
            setAgnosticValue("savedLayoutVersion", version);
            ModConfig.CLIENT_SPEC.save();
        } catch (Exception e) {
            LogManager.error("Failed to save layout version to configuration!", e);
        }
    }

    /**
     * Saves the highest server-triggered global reset version this client
     * has applied, so it isn't reset again for the same admin-triggered
     * reset on a later login.
     *
     * @param version the server reset version to record
     */
    public static void saveLastAppliedServerResetVersion(int version) {
        try {
            setAgnosticValue("lastAppliedServerResetVersion", version);
            ModConfig.CLIENT_SPEC.save();
        } catch (Exception e) {
            LogManager.error("Failed to save last applied server reset version to configuration!", e);
        }
    }

    /**
     * Resets the dashboard to its default tab layout and default widget
     * placements, then persists that reset immediately. Used by every
     * reset trigger — first-run, a layout schema version bump, and
     * server-pushed resets — so there's a single source of truth for what
     * "reset to defaults" actually does.
     */
    public static void resetToDefaults() {
        try {
            com.drultralux.townsteadfactions.client.screen.TabManager.installDefaultLayout();

            setAgnosticValue("treasuryWidgetTabId", com.drultralux.townsteadfactions.client.screen.TabManager.DEFAULT_TAB_OVERVIEW);
            setAgnosticValue("rosterWidgetTabId", com.drultralux.townsteadfactions.client.screen.TabManager.DEFAULT_TAB_ROSTER);
            setAgnosticValue("globalWidgetTabId", com.drultralux.townsteadfactions.client.screen.TabManager.DEFAULT_TAB_GLOBAL);
            setAgnosticValue("avatarWidgetTabId", com.drultralux.townsteadfactions.client.screen.TabManager.DEFAULT_TAB_OVERVIEW);
            setAgnosticValue("activityWidgetTabId", com.drultralux.townsteadfactions.client.screen.TabManager.DEFAULT_TAB_OVERVIEW);

            List<String> encodedTabs = new ArrayList<>();
            for (TabPanelWidget tab : com.drultralux.townsteadfactions.client.screen.TabManager.getTabs()) {
                encodedTabs.add(tab.getId() + ";" + tab.getTitle());
            }
            setAgnosticValue("customizedTabOrder", encodedTabs);

            ModConfig.CLIENT_SPEC.save();
            LogManager.info("Faction dashboard layout reset to defaults.");
        } catch (Exception e) {
            LogManager.error("Failed to reset dashboard layout to defaults!", e);
        }
    }

    /**
     * Updates a single value in the client config registry by key, if a
     * config value exists for that key.
     *
     * @param key the config key to update
     * @param value the new value to set
     * @param <T> the type of the config value
     */
    @SuppressWarnings("unchecked")
    private static <T> void setAgnosticValue(String key, T value) {
        if (ModConfig.CLIENT.valuesRegistry.containsKey(key)) {
            ((ModConfigSpec.ConfigValue<T>) ModConfig.CLIENT.valuesRegistry.get(key)).set(value);
        }
    }
}