package com.drultralux.townsteadfactions.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * Centrally manages keyboard binding registrations for the Factions user interface.
 * Sets up safe operational conflict rules specifically isolated to standard in-game play contexts.
 */
public class KeyMappings {
    /** The descriptive local translation string identifier for the key binding category. */
    private static final String KEY_CATEGORY = "key.categories.townsteadfactions";
    /** The explicit localized mapping reference key identifier for the dashboard screen. */
    private static final String KEY_DASHBOARD = "key.townsteadfactions.dashboard";

    /** Dynamic mapping registry tracker holding the active keyboard trigger binding. */
    public static final KeyMapping OPEN_FACTION_DASHBOARD = new KeyMapping(
            KEY_DASHBOARD,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O, // Restored back to the 'O' letter key shortcut
            KEY_CATEGORY
    );
}