package com.drultralux.townstead_factions.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public class KeyMappings {

    public static final KeyMapping OPEN_FACTION_MENU = new KeyMapping(
            "key.townstead_factions.open_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F,
            "key.categories.townstead_factions"
    );

    public void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_FACTION_MENU);
    }
}
