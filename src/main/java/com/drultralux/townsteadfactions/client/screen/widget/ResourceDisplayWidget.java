package com.drultralux.townsteadfactions.client.screen.widget;

import com.drultralux.townsteadfactions.client.ClientFactionCache;
import com.drultralux.townsteadfactions.client.ClientFactionCache.ClientFactionData;
import dev.marie.MariesLib.client.GuiValueRenderer;
import dev.marie.MariesLib.client.MarieValueColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A specialized draggable sub-window panel element that displays synchronized live economy counts.
 * Leverages MariesLib color palettes to render standardized segmented indicator blocks.
 */
public class ResourceDisplayWidget extends DraggableWidget {
    private final Font font;

    /**
     * Builds out our treasury display window profile utilizing fixed layout size parameters.
     *
     * @param x the initial horizontal rendering coordinate tracking vector
     * @param y the initial vertical rendering coordinate tracking vector
     */
    public ResourceDisplayWidget(int x, int y) {
        super(x, y, 220, 100);
        this.font = Minecraft.getInstance().font;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, GuiValueRenderer barRenderer) {
        // 1. COLLAPSED MINIMIZED VIEWPORT RENDERING ROUTINE
        if (this.isMinimized) {
            int headerColor = this.isDragging ? 0xEE444444 : 0xEE222222;
            graphics.fill(this.x, this.y, this.x + this.width, this.y + 14, headerColor);
            graphics.renderOutline(this.x, this.y, this.width, 14, 0xFF555555);

            String minBtnText = isMinimizeButtonHovered(mouseX, mouseY) ? "§e[+]" : "§7[+]";
            graphics.drawString(this.font, minBtnText, this.x + this.width - 16, this.y + 3, 0xFFFFFF, false);
            graphics.drawString(this.font, "§6§lTREASURY (MINIMIZED)", this.x + 6, this.y + 3, 0xFFFFFF, false);
            return;
        }

        // 2. STANDARD EXPANDED VIEWPORT RENDERING ROUTINE
        int backgroundColor = this.isDragging ? 0xAA444444 : 0xAA222222;
        graphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, backgroundColor);
        graphics.renderOutline(this.x, this.y, this.width, this.height, 0xFF666666);

        String minBtnText = isMinimizeButtonHovered(mouseX, mouseY) ? "§e[-]" : "§7[-]";
        graphics.drawString(this.font, minBtnText, this.x + this.width - 16, this.y + 3, 0xFFFFFF, false);

        // Fetch live values straight out of our optimized client data snapshot mapping matrix
        String activeId = ClientFactionCache.getAssignedFactionId();
        ClientFactionData faction = ClientFactionCache.getCachedFactions().get(activeId);

        int factionSize = (faction != null) ? faction.roster.size() : 0;
        int liveCogs = (faction != null) ? faction.cogs : 0;
        int liveFood = (faction != null) ? faction.food : 0;
        int liveMana = (faction != null) ? faction.mana : 0;

        // Fetch our explicit color registrations natively from the active MariesLib palette engine
        int powerColor = MarieValueColors.baseColorArgb("power");
        int shipsColor = MarieValueColors.baseColorArgb("ships");
        int cogsColor = MarieValueColors.baseColorArgb("gold");
        int foodColor = MarieValueColors.baseColorArgb("food");
        int manaColor = MarieValueColors.baseColorArgb("mana");

        // --- Left Data Column Layer Elements ---
        graphics.drawString(this.font, "§b§lPOWER & MILITARY", this.x + 6, this.y + 6, 0xFFFFFF, false);
        graphics.drawString(this.font, "Members: " + factionSize, this.x + 8, this.y + 18, 0xAAAAAA, false);

        // Render Segmented Member Squares Grid via the custom custom method segmentRenderer
        renderSegmentedBlocks(graphics, this.x + 8, this.y + 28, (factionSize > 0) ? factionSize / 100.0f : 0.0f, powerColor);

        graphics.drawString(this.font, "Airships Active", this.x + 8, this.y + 42, 0xAAAAAA, false);
        renderSegmentedBlocks(graphics, this.x + 8, this.y + 52, 0.5f, shipsColor);

        // --- Right Data Column Layer Elements ---
        int col2X = this.x + 115;
        graphics.drawString(this.font, "§e§lTREASURY LEDGER", col2X, this.y + 6, 0xFFFFFF, false);

        graphics.drawString(this.font, "Cogs: " + liveCogs, col2X, this.y + 18, 0xAAAAAA, false);
        renderSegmentedBlocks(graphics, col2X, this.y + 28, liveCogs / 10.0f, cogsColor);

        graphics.drawString(this.font, "Food Supplies", col2X, this.y + 42, 0xAAAAAA, false);
        renderSegmentedBlocks(graphics, col2X, this.y + 52, liveFood / 10.0f, foodColor);

        graphics.drawString(this.font, "Mana Stockpile", col2X, this.y + 66, 0xAAAAAA, false);
        renderSegmentedBlocks(graphics, col2X, this.y + 76, liveMana / 10.0f, manaColor);
    }

    /**
     * Localized method to cleanly render MariesLib style segmented progress indicator grids.
     */
    private void renderSegmentedBlocks(GuiGraphics graphics, int targetX, int targetY, float fillLevel, int barColor) {
        int filledCount = (int) (fillLevel * 10);
        for (int i = 0; i < 10; i++) {
            int blockColor = (i < filledCount) ? barColor : 0xFF2A2A2A;
            int renderX = targetX + (i * 8);
            graphics.fill(renderX, targetY, renderX + 6, targetY + 6, blockColor);
        }
    }
}