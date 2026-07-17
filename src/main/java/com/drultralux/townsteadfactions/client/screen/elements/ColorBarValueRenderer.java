package com.drultralux.townsteadfactions.client.screen.elements;

import dev.marie.MariesLib.client.GuiValueRenderer;
import net.minecraft.client.gui.GuiGraphics;

public class ColorBarValueRenderer implements GuiValueRenderer {
    private final int barColor;

    public ColorBarValueRenderer(int barColor) {
        this.barColor = barColor;
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, float level) {
        int filledCount = (int) (level * 10);
        for (int i = 0; i < 10; i++) {
            int currentBlockColor = (i < filledCount) ? this.barColor : 0xFF2A2A2A;
            int renderX = x + (i * 8);
            graphics.fill(renderX, y, renderX + 6, y + 6, currentBlockColor);
        }
    }
}
