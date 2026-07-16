package com.drultralux.townsteadfactions.client.screen;

import com.drultralux.townsteadfactions.LogManager;
import com.drultralux.townsteadfactions.config.ModConfig;

/**
 * Handles basic reading and writing of window positions to the client configuration.
 */
public class ScreenLayoutSaver {

    /**
     * Commits coordinate locations and frame states for your widgets directly down to disk configuration.
     */
    public static void saveWidgetLayout(int tx, int ty, int tTab, int rx, int ry, int rTab, int gx, int gy, int gTab, int boxW, int boxH) {
        try {
            setAgnosticValue("treasuryWidgetX", tx);
            setAgnosticValue("treasuryWidgetY", ty);
            setAgnosticValue("treasuryWidgetTab", tTab);

            setAgnosticValue("rosterWidgetX", rx);
            setAgnosticValue("rosterWidgetY", ry);
            setAgnosticValue("rosterWidgetTab", rTab);

            setAgnosticValue("globalWidgetX", gx);
            setAgnosticValue("globalWidgetY", gy);
            setAgnosticValue("globalWidgetTab", gTab);

            setAgnosticValue("mainBoxWidth", boxW);
            setAgnosticValue("mainBoxHeight", boxH);

            // Saves the newly modified configuration specification directly onto your hard drive
            ModConfig.CLIENT_SPEC.save();
            LogManager.debug("Dynamic window layouts successfully committed to disk files.");
        } catch (Exception e) {
            LogManager.error("Failed to commit operational screen coordinates to configuration!", e);
        }
    }

    /**
     * Internal utility method designed to update values inside our agnostic config registry.
     */
    @SuppressWarnings("unchecked")
    private static <T> void setAgnosticValue(String key, T value) {
        if (ModConfig.CLIENT.valuesRegistry.containsKey(key)) {
            ((net.neoforged.neoforge.common.ModConfigSpec.ConfigValue<T>) ModConfig.CLIENT.valuesRegistry.get(key)).set(value);
        }
    }
}