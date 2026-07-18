package com.drultralux.townsteadfactions.client.screen;

import com.drultralux.townsteadfactions.utils.LogManager;
import com.drultralux.townsteadfactions.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Handles reading and writing of faction dashboard window and widget
 * positions to the client configuration file.
 */
public class ScreenLayoutSaver {

    /**
     * Saves the current position, tab assignment, and size of every
     * draggable dashboard widget to the client config, then writes the
     * config to disk.
     *
     * @param tx treasury widget x position
     * @param ty treasury widget y position
     * @param tTab treasury widget tab index
     * @param rx roster widget x position
     * @param ry roster widget y position
     * @param rTab roster widget tab index
     * @param gx global widget x position
     * @param gy global widget y position
     * @param gTab global widget tab index
     * @param ax avatar widget x position
     * @param ay avatar widget y position
     * @param aTab avatar widget tab index
     * @param lx activity log widget x position
     * @param ly activity log widget y position
     * @param lTab activity log widget tab index
     * @param boxW main dashboard window width
     * @param boxH main dashboard window height
     */
    public static void saveWidgetLayout(int tx, int ty, int tTab, int rx, int ry, int rTab, int gx, int gy, int gTab, int ax, int ay, int aTab, int lx, int ly, int lTab, int boxW, int boxH) {
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

            setAgnosticValue("avatarWidgetX", ax);
            setAgnosticValue("avatarWidgetY", ay);
            setAgnosticValue("avatarWidgetTab", aTab);

            setAgnosticValue("activityWidgetX", lx);
            setAgnosticValue("activityWidgetY", ly);
            setAgnosticValue("activityWidgetTab", lTab);

            setAgnosticValue("mainBoxWidth", boxW);
            setAgnosticValue("mainBoxHeight", boxH);

            ModConfig.CLIENT_SPEC.save();
            LogManager.debug("Dynamic window layouts successfully committed to disk files.");
        } catch (Exception e) {
            LogManager.error("Failed to commit operational screen coordinates to configuration!", e);
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