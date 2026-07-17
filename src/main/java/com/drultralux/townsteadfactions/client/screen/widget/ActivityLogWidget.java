package com.drultralux.townsteadfactions.client.screen.widget;

import dev.marie.MariesLib.client.GuiValueRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogWidget extends DraggableWidget {
    private final Font font;
    private final List<String> activityLog = new ArrayList<>();

    public ActivityLogWidget(int x, int y) {
        // Optimized box width from 260 down to 210 to match layout limits cleanly
        super(x, y, 210, 60);
        this.font = Minecraft.getInstance().font;

        this.activityLog.add("• DrUltraLux opened the interface screen.");
        this.activityLog.add("• Server payload channel synchronized.");
        this.activityLog.add("• Local capability profiles initialized.");
        this.activityLog.add("• Database registries parsed cleanly.");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, GuiValueRenderer barRenderer) {
        graphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, 0x66000000);
        graphics.renderOutline(this.x, this.y, this.x + this.width, this.y + this.height, 0xFF444444);

        if (this.isMinimized) return;

        String logHeader = "⚙ FACTION LOG";
        graphics.drawString(this.font, Component.literal(logHeader), this.x + 6, this.y + 5, 0xFFA200, false);
        graphics.fill(this.x + 4, this.y + 14, this.x + this.width - 4, this.y + 15, 0xFF333333);

        int currentYOffset = this.y + 18;
        for (String logLine : this.activityLog) {
            graphics.drawString(this.font, Component.literal(logLine), this.x + 8, currentYOffset, 0x999999, false);
            currentYOffset += 9;
            if (currentYOffset >= this.y + this.height - 6) break;
        }
    }
}