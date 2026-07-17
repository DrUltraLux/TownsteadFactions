package com.drultralux.townsteadfactions.client.screen.widget;

import com.drultralux.townsteadfactions.LogManager;
import com.drultralux.townsteadfactions.client.ClientFactionCache;
import com.drultralux.townsteadfactions.client.screen.FactionPalette;
import com.drultralux.townsteadfactions.client.screen.elements.ColorBarValueRenderer;
import com.drultralux.townsteadfactions.client.screen.elements.TextLineValueRenderer;
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

        this.powerHeader = new TextLineValueRenderer(Minecraft.getInstance().font, "§bPOWER", FactionPalette.getBarColor( "text_pink"));
        this.airshipsLabel = new TextLineValueRenderer(Minecraft.getInstance().font, "Airships", FactionPalette.getBarColor( "text_pink"));
        this.resourcesHeader = new TextLineValueRenderer(Minecraft.getInstance().font, "§eRESOURCES", FactionPalette.getBarColor( "text_gold"));
        this.treasuryLabel = new TextLineValueRenderer(Minecraft.getInstance().font, "Treasury:", FactionPalette.getBarColor( "gold"));
        this.foodLabel = new TextLineValueRenderer(Minecraft.getInstance().font, "Food Supplies", FactionPalette.getBarColor( "food"));
        this.manaLabel = new TextLineValueRenderer(Minecraft.getInstance().font, "Mana Stockpile", FactionPalette.getBarColor( "mana"));

        this.powerBar = new ColorBarValueRenderer(FactionPalette.getBarColor( "power"));
        this.airshipsBar = new ColorBarValueRenderer(FactionPalette.getBarColor( "ships"));
        this.treasuryBar = new ColorBarValueRenderer(FactionPalette.getBarColor( "gold"));
        this.foodBar = new ColorBarValueRenderer(FactionPalette.getBarColor( "food"));
        this.manaBar = new ColorBarValueRenderer(FactionPalette.getBarColor( "mana"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicksr) {
        //Maintain minimize window and draggable background logic
        if (this.isMinimized) {
            int headerColor = this.isDragging ? 0xEE444444 : 0xEE222222;
            graphics.fill(this.x, this.y, this.x + this.width, this.y + 14, headerColor);
            graphics.renderOutline(this.x, this.y, this.width, 14, 0xFF555555);

            String minBtnText = isMinimizeButtonHovered(mouseX, mouseY) ? "§e[+]" : "§7[+]";
            graphics.drawString(this.font, minBtnText, this.x + this.width - 16, this.y + 3, FactionPalette.getBarColor( "text_gold"), false);
            graphics.drawString(this.font, "§6§lRESOURCES (MIN)", this.x + 6, this.y + 3, FactionPalette.getBarColor( "text_gold"), false);
            return;
        }

        int backgroundColor = this.isDragging ? 0xAA444444 : 0xAA222222;
        graphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, backgroundColor);
        graphics.renderOutline(this.x, this.y, this.width, this.height, 0xFF666666);

        String minBtnText = isMinimizeButtonHovered(mouseX, mouseY) ? "§e[-]" : "§7[-]";
        graphics.drawString(this.font, minBtnText, this.x + this.width - 16, this.y + 3, FactionPalette.getBarColor( "text_gold"), false);

        //Fetch Live Cache Values
        String activeId = ClientFactionCache.getAssignedFactionId();
        ClientFactionCache.ClientFactionData faction = ClientFactionCache.getCachedFactions().get(activeId);

        int factionSize = (faction != null) ? faction.roster.size() : 0;
        int liveCogs = ClientFactionCache.getCogs();
        int liveFood = ClientFactionCache.getFood();
        int liveMana = ClientFactionCache.getMana();

        float powerPercent = (factionSize > 0) ? 1.0F : 0.0F;
        float shipsPercent = 0.8F;
        float cogsPercent = Math.min(1.0F, Math.max(0.0F, (float) liveCogs / 10.0F));
        float foodPercent = Math.min(1.0F, Math.max(0.0F, (float) liveFood / 10.0F));
        float manaPercent = Math.min(1.0F, Math.max(0.0F, (float) liveMana / 10.0F));

        LogManager.info("Cache Diagnostic -> Cogs: {"+liveCogs+"}");
        LogManager.info("Cache Diagnostic -> Food: {"+liveFood+"}");
        LogManager.info("Cache Diagnostic -> Mana: {"+liveMana+"}");

        // ==========================================
        // --- COLUMN 1: LEFT SIDE (POWER & AIRSHIPS)
        // ==========================================
        int col1X = this.x + 8;

        this.powerHeader.render(graphics, col1X, this.y + 6, 0.0F);
        graphics.drawString(this.font, Component.literal("Power: " + factionSize + " / " + factionSize), col1X, this.y + 18, FactionPalette.getBarColor( "text_pink"), false);
        this.powerBar.render(graphics, col1X, this.y + 28, powerPercent);

        this.airshipsLabel.render(graphics, col1X, this.y + 44, 0.0F);
        this.airshipsBar.render(graphics, col1X, this.y + 54, shipsPercent);

        // ==========================================
        // --- COLUMN 2: RIGHT SIDE (STATISTICS & STORAGE)
        // ==========================================
        int col2X = this.x + 115;

        this.resourcesHeader.render(graphics, col2X, this.y + 6, 0.0F);

        this.treasuryLabel.render(graphics, col2X, this.y + 18, 0.0F);
        this.treasuryBar.render(graphics, col2X, this.y + 28, cogsPercent);

        this.foodLabel.render(graphics, col2X, this.y + 42, 0.0F);
        this.foodBar.render(graphics, col2X, this.y + 52, foodPercent);

        this.manaLabel.render(graphics, col2X, this.y + 66, 0.0F);
        this.manaBar.render(graphics, col2X, this.y + 76, manaPercent);
    }
}