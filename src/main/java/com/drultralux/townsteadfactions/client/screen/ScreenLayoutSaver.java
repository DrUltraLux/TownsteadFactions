package com.drultralux.townsteadfactions.client.screen;

import com.drultralux.townsteadfactions.LogManager;
import com.drultralux.townsteadfactions.config.ModConfig;

/**
 * Handles basic reading and writing of window positions to the client configuration.
 */
public class ScreenLayoutSaver {

    /**
     * Saves the current position of the treasury widget straight to the config values.
     *
     * @param x the horizontal position pixel coordinate vector
     * @param y the vertical position pixel coordinate vector
     * @param tabIndex the active tab panel index tracking selection
     */
    public static void saveTreasuryPosition(int x, int y, int tabIndex) {
        try {
            ModConfig.CLIENT.treasuryWidgetX.set(x);
            ModConfig.CLIENT.treasuryWidgetY.set(y);
            ModConfig.CLIENT.treasuryWidgetTab.set(tabIndex);

            // Forces NeoForge to flush changes to the townsteadfactions-client.toml file
            ModConfig.CLIENT_SPEC.save();
            LogManager.debug("Treasury widget position cached: Tab " + tabIndex + " (" + x + ", " + y + ")");
        } catch (Exception e) {
            LogManager.error("Failed to save layout positions to client config!", e);
        }
    }
}
