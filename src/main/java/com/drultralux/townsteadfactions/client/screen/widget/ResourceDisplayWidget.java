package com.drultralux.townsteadfactions.client.screen.widget;

import com.drultralux.townsteadfactions.client.ClientFactionCache;
import com.drultralux.townsteadfactions.client.ClientFactionCache.ClientFactionData;
import dev.marie.MariesLib.client.GuiValueRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A specialized draggable sub-window panel element that displays synchronized live economy counts.
 * Integrates directly with MariesLib functional layout renderers to condense segmentation rendering pipelines.
 */
public class ResourceDisplayWidget extends DraggableWidget {
    private final Font font;

    /**
     * Allocates operational dimensions and pulls system fonts to prepare the economic rendering grid panel.
     *
     * @param x structural horizontal pixel grid allocation location
     * @param y structural vertical pixel grid allocation location
     */
    public ResourceDisplayWidget(int x, int y) {
        super(x, y, 300, 95);
        this.font = Minecraft.getInstance().font;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, GuiValueRenderer barRenderer) {
        if (this.isMinimized) {
            int headerColor = this.isDragging ? 0xEE444444 : 0xEE222222;
            graphics.fill(this.x, this.y, this.x + this.width, this.y + 14, headerColor);
            graphics.renderOutline(this.x, this.y, this.width, 14, 0xFF555555);

            String minBtnText = isMinimizeButtonHovered(mouseX, mouseY) ? "§e[+]" : "§7[+]";
            graphics.drawString(this.font, minBtnText, this.x + this.width - 16, this.y + 3, 0xFFFFFF, false);
            graphics.drawString(this.font, "§6§lTREASURY PROFILE (COLLAPSED)", this.x + 6, this.y + 3, 0xFFFFFF, false);
            return;
        }

        int backgroundColor = this.isDragging ? 0xAA444444 : 0xAA222222;
        graphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, backgroundColor);
        graphics.renderOutline(this.x, this.y, this.width, this.height, 0xFF666666);

        String minBtnText = isMinimizeButtonHovered(mouseX, mouseY) ? "§e[-]" : "§7[-]";
        graphics.drawString(this.font, minBtnText, this.x + this.width - 16, this.y + 3, 0xFFFFFF, false);

        String activeId = ClientFactionCache.getAssignedFactionId();
        ClientFactionData faction = ClientFactionCache.getCachedFactions().get(activeId);

        String factionName = (faction != null) ? faction.name : "No Faction";
        int factionSize = (faction != null) ? faction.roster.size() : 0;
        int liveCogs = (faction != null) ? faction.cogs : 0;
        int liveFood = (faction != null) ? faction.food : 0;
        int liveMana = (faction != null) ? faction.mana : 0;

        // --- Column 1 Parameters: Structural Overviews ---
        int col1X = this.x + 8;
        graphics.drawString(this.font, "§6§l" + factionName.toUpperCase(), col1X, this.y + 8, 0xFFFFFF, false);
        graphics.drawString(this.font, "Power: " + factionSize + " Members Active", col1X, this.y + 22, 0xAAAAAA, false);

        // Render Members/Power Segment Block Bar (Local flag value context = 0)
        // This targets the first segment color parameter map inside the screen's rendering intercept block
        graphics.fill(this.x, this.y + 34, this.x + 1, this.y + 35, 0);
        barRenderer.render(graphics, col1X, this.y + 34, (factionSize > 0) ? factionSize / 100.0f : 0.0f);

        graphics.drawString(this.font, "§d§lAIRSHIPS CAPACITY", col1X, this.y + 48, 0xFFFFFF, false);
        // Render Airships Segment Block Bar (Local flag value context = 1)
        graphics.fill(this.x, this.y + 60, this.x + 1, this.y + 61, 1);
        barRenderer.render(graphics, col1X, this.y + 60, 0.9f);

        // --- Column 2 Parameters: Live Currency Accounts ---
        int col2X = this.x + 155;
        graphics.drawString(this.font, "§6§lTREASURY ACCOUNTS", col2X, this.y + 8, 0xFFFFFF, false);

        graphics.drawString(this.font, "Cogs Asset Ledger:", col2X, this.y + 22, 0xAAAAAA, false);
        graphics.fill(this.x, this.y + 32, this.x + 1, this.y + 33, 2);
        barRenderer.render(graphics, col2X, this.y + 32, liveCogs / 10.0f);

        graphics.drawString(this.font, "Food Provision Reserves:", col2X, this.y + 44, 0xAAAAAA, false);
        graphics.fill(this.x, this.y + 54, this.x + 1, this.y + 55, 3);
        barRenderer.render(graphics, col2X, this.y + 54, liveFood / 10.0f);

        graphics.drawString(this.font, "Mana Essence stock:", col2X, this.y + 66, 0xAAAAAA, false);
        graphics.fill(this.x, this.y + 76, this.x + 1, this.y + 77, 4);
        barRenderer.render(graphics, col2X, this.y + 76, liveMana / 10.0f);
    }
}