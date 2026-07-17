package com.drultralux.townsteadfactions.client.screen.elements;

import dev.marie.MariesLib.client.GuiValueRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class TextLineValueRenderer implements GuiValueRenderer {
    private final Font font;
    private final String text;
    private final int color;

    public TextLineValueRenderer(Font font, String text, int color) {
        this.font = font;
        this.text = text;
        this.color = color;
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, float level) {
        graphics.drawString(this.font, Component.literal(this.text), x, y, this.color, false);
    }
}