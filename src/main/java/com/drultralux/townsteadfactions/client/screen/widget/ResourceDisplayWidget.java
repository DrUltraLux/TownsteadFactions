package com.drultralux.townsteadfactions.client.screen.widget;

import com.drultralux.townsteadfactions.client.ClientFactionCache;
import com.drultralux.townsteadfactions.client.screen.elements.ColorBarValueRenderer;
import com.drultralux.townsteadfactions.client.screen.elements.TextLineValueRenderer;
import dev.marie.MariesLib.client.MarieValueColors;
import dev.marie.MariesLib.client.GuiValueRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class ResourceDisplayWidget extends DraggableWidget {
    private final TextLineValueRenderer powerHeader, resourcesHeader, airshipsLabel, treasuryLabel, foodLabel, manaLabel;
    private final ColorBarValueRenderer powerBar, airshipsBar, treasuryBar, foodBar, manaBar;
    private final Font font;

    public ResourceDisplayWidget(int x, int y) {
        super(x, y, 220, 100);
        this.font = Minecraft.getInstance().font;
        // Pre-cache persistent text and colors to avoid GC pressure
        this.powerHeader = new TextLineValueRenderer(Minecraft.getInstance().font, "§bPOWER", 0xFFFFFF);
        this.airshipsLabel = new TextLineValueRenderer(Minecraft.getInstance().font, "Airships", 0xAAAAAA);
        this.resourcesHeader = new TextLineValueRenderer(Minecraft.getInstance().font, "§eRESOURCES", 0xFFFFFF);
        this.treasuryLabel = new TextLineValueRenderer(Minecraft.getInstance().font, "Treasury:", 0xAAAAAA);
        this.foodLabel = new TextLineValueRenderer(Minecraft.getInstance().font, "Food Supplies", 0xAAAAAA);
        this.manaLabel = new TextLineValueRenderer(Minecraft.getInstance().font, "Mana Stockpile", 0xAAAAAA);

        this.powerBar = new ColorBarValueRenderer(MarieValueColors.baseColorArgb("power"));
        this.airshipsBar = new ColorBarValueRenderer(MarieValueColors.baseColorArgb("ships"));
        this.treasuryBar = new ColorBarValueRenderer(MarieValueColors.baseColorArgb("gold"));
        this.foodBar = new ColorBarValueRenderer(MarieValueColors.baseColorArgb("food"));
        this.manaBar = new ColorBarValueRenderer(MarieValueColors.baseColorArgb("mana"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, GuiValueRenderer barRenderer) {
        // 1. Maintain minimize window and draggable background logic
        if (this.isMinimized) {
            int headerColor = this.isDragging ? 0xEE444444 : 0xEE222222;
            graphics.fill(this.x, this.y, this.x + this.width, this.y + 14, headerColor);
            graphics.renderOutline(this.x, this.y, this.width, 14, 0xFF555555);

            String minBtnText = isMinimizeButtonHovered(mouseX, mouseY) ? "§e[+]" : "§7[+]";
            graphics.drawString(this.font, minBtnText, this.x + this.width - 16, this.y + 3, 0xFFFFFF, false);
            graphics.drawString(this.font, "§6§lSTATISTICS & STORAGE (MIN)", this.x + 6, this.y + 3, 0xFFFFFF, false);
            return;
        }

        int backgroundColor = this.isDragging ? 0xAA444444 : 0xAA222222;
        graphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, backgroundColor);
        graphics.renderOutline(this.x, this.y, this.width, this.height, 0xFF666666);

        String minBtnText = isMinimizeButtonHovered(mouseX, mouseY) ? "§e[-]" : "§7[-]";
        graphics.drawString(this.font, minBtnText, this.x + this.width - 16, this.y + 3, 0xFFFFFF, false);

        // 2. Fetch Live Cache Values
        String activeId = ClientFactionCache.getAssignedFactionId();
        ClientFactionCache.ClientFactionData faction = ClientFactionCache.getCachedFactions().get(activeId);

        int factionSize = (faction != null) ? faction.roster.size() : 0;
        int liveCogs = ClientFactionCache.getCogs();
        int liveFood = ClientFactionCache.getFood();
        int liveMana = ClientFactionCache.getMana();

        // 3. Calculate Percentages for the ColorBar Value Renderers
        // For Power, we use 1/1 if faction size is populated, matching your target view
        float powerPercent = (factionSize > 0) ? 1.0F : 0.0F;
        float shipsPercent = 0.8F; // Set to match your target layout visual level
        float cogsPercent = Math.min(1.0F, Math.max(0.0F, (float) liveCogs / 10.0F));
        float foodPercent = Math.min(1.0F, Math.max(0.0F, (float) liveFood / 10.0F));
        float manaPercent = Math.min(1.0F, Math.max(0.0F, (float) liveMana / 10.0F));

        // ==========================================
        // --- COLUMN 1: LEFT SIDE (POWER & AIRSHIPS)
        // ==========================================
        int col1X = this.x + 8;

        // Power Section (Text Header, then Bar pushed 10px down)
        this.powerHeader.render(graphics, col1X, this.y + 6, 0.0F);
        // Draw the subtext label "Power: 1 / 1" matching screenshot 2
        graphics.drawString(this.font, Component.literal("Power: " + factionSize + " / " + factionSize), col1X, this.y + 18, 0xAAAAAA, false);
        this.powerBar.render(graphics, col1X, this.y + 28, powerPercent);

        // Airships Section
        this.airshipsLabel.render(graphics, col1X, this.y + 44, 0.0F);
        this.airshipsBar.render(graphics, col1X, this.y + 54, shipsPercent);

        // ==========================================
        // --- COLUMN 2: RIGHT SIDE (STATISTICS & STORAGE)
        // ==========================================
        int col2X = this.x + 115;

        // "STATISTICS & STORAGE" Header
        this.resourcesHeader.render(graphics, col2X, this.y + 6, 0.0F);

        // Treasury / Cogs Row (Header text at y+18, Bar at y+28)
        this.treasuryLabel.render(graphics, col2X, this.y + 18, 0.0F);
        this.treasuryBar.render(graphics, col2X, this.y + 28, cogsPercent);

        // Food Supplies Row (Header text at y+42, Bar at y+52)
        this.foodLabel.render(graphics, col2X, this.y + 42, 0.0F);
        this.foodBar.render(graphics, col2X, this.y + 52, foodPercent);

        // Mana Stockpile Row (Header text at y+66, Bar at y+76)
        this.manaLabel.render(graphics, col2X, this.y + 66, 0.0F);
        this.manaBar.render(graphics, col2X, this.y + 76, manaPercent);
    }
}