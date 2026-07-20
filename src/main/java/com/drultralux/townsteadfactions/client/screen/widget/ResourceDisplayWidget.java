package com.drultralux.townsteadfactions.client.screen.widget;

import com.drultralux.townsteadfactions.utils.LogManager;
import com.drultralux.townsteadfactions.client.ClientFactionCache;
import com.drultralux.townsteadfactions.client.screen.FactionPalette;
import com.drultralux.townsteadfactions.client.screen.elements.ColorBarValueRenderer;
import com.drultralux.townsteadfactions.client.screen.elements.TextLineValueRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Map;

/**
 * A draggable widget displaying the local player's faction resources:
 * power, airships, treasury, food, and mana. Power and treasury are
 * shown as this faction's share of the server-wide total, computed
 * client-side from the full set of factions already present in
 * {@link ClientFactionCache} — no dedicated network data needed.
 */
public class ResourceDisplayWidget extends DraggableWidget {

    /** Text renderers for each section header and label. */
    private final TextLineValueRenderer powerHeader, resourcesHeader, airshipsLabel, treasuryLabel, foodLabel, manaLabel;
    private final TextLineValueRenderer territoryLabel;

    /** Bar renderers for each resource's fill level. */
    private final ColorBarValueRenderer powerBar, airshipsBar, treasuryBar, foodBar, manaBar;

    /** The font used to draw labels and values. */
    private final Font font;

    /**
     * Creates the resource display widget at the given position with a
     * fixed default size, initializing all its labels and bars.
     *
     * @param x the x position of the widget
     * @param y the y position of the widget
     */
    public ResourceDisplayWidget(int x, int y) {
        super(x, y, 220, 100);
        this.font = Minecraft.getInstance().font;

        this.powerHeader = new TextLineValueRenderer(Minecraft.getInstance().font, "§bPOWER", FactionPalette.getBarColor("text_pink"));
        this.airshipsLabel = new TextLineValueRenderer(Minecraft.getInstance().font, "Airships", FactionPalette.getBarColor("text_pink"));
        this.resourcesHeader = new TextLineValueRenderer(Minecraft.getInstance().font, "§eRESOURCES", FactionPalette.getBarColor("text_gold"));
        this.treasuryLabel = new TextLineValueRenderer(Minecraft.getInstance().font, "Treasury:", FactionPalette.getBarColor("gold"));
        this.foodLabel = new TextLineValueRenderer(Minecraft.getInstance().font, "Food Supplies", FactionPalette.getBarColor("food"));
        this.manaLabel = new TextLineValueRenderer(Minecraft.getInstance().font, "Mana Stockpile", FactionPalette.getBarColor("mana"));
        this.territoryLabel = new TextLineValueRenderer(Minecraft.getInstance().font, "Territory", FactionPalette.getBarColor("text_pink"));

        this.powerBar = new ColorBarValueRenderer(FactionPalette.getBarColor("power"));
        this.airshipsBar = new ColorBarValueRenderer(FactionPalette.getBarColor("ships"));
        this.treasuryBar = new ColorBarValueRenderer(FactionPalette.getBarColor("gold"));
        this.foodBar = new ColorBarValueRenderer(FactionPalette.getBarColor("food"));
        this.manaBar = new ColorBarValueRenderer(FactionPalette.getBarColor("mana"));
    }

    /**
     * Renders the widget's minimized header or, when expanded, its full
     * two-column layout of resource labels and bars, pulling current
     * values from {@link ClientFactionCache}.
     *
     * @param graphics the graphics context to draw with
     * @param mouseX the current mouse x position
     * @param mouseY the current mouse y position
     * @param partialTicks the partial tick time, for frame interpolation
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Minimized: draw only the header bar with a re-expand button
        if (this.isMinimized) {
            int headerColor = this.isDragging ? 0xEE444444 : 0xEE222222;
            graphics.fill(this.x, this.y, this.x + this.width, this.y + 14, headerColor);
            graphics.renderOutline(this.x, this.y, this.width, 14, 0xFF555555);

            String minBtnText = isMinimizeButtonHovered(mouseX, mouseY) ? "§e[+]" : "§7[+]";
            graphics.drawString(this.font, minBtnText, this.x + this.width - 16, this.y + 3, FactionPalette.getBarColor("text_gold"), false);
            graphics.drawString(this.font, "§6§lRESOURCES (MIN)", this.x + 6, this.y + 3, FactionPalette.getBarColor("text_gold"), false);
            return;
        }

        int backgroundColor = this.isDragging ? 0xAA444444 : 0xAA222222;
        graphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, backgroundColor);
        graphics.renderOutline(this.x, this.y, this.width, this.height, 0xFF666666);

        renderMinimizeButton(graphics, this.font);

        String activeId = ClientFactionCache.getAssignedFactionId();
        Map<String, ClientFactionCache.ClientFactionData> allFactions = ClientFactionCache.getCachedFactions();
        ClientFactionCache.ClientFactionData faction = allFactions.get(activeId);

        int factionSize = (faction != null) ? faction.roster.size() + faction.villagerCount : 0;
        int liveCogs = ClientFactionCache.getCogs();
        int liveFood = ClientFactionCache.getFood();
        int liveMana = ClientFactionCache.getMana();

        // Power and Treasury are this faction's share of the server-wide total across every
        // active faction (already fully present in the client cache — no extra sync needed).
        int totalPlayerCount = 0;
        int totalCogs = 0;
        for (ClientFactionCache.ClientFactionData f : allFactions.values()) {
            totalPlayerCount += f.roster.size() + f.villagerCount;
            totalCogs += f.cogs;
        }

        float powerPercent = (totalPlayerCount > 0) ? (float) factionSize / totalPlayerCount : 0.0F;
        float shipsPercent = 0.8F;
        float cogsPercent = (totalCogs > 0) ? (float) liveCogs / totalCogs : 0.0F;
        float foodPercent = Math.min(1.0F, Math.max(0.0F, (float) liveFood / 10.0F));
        float manaPercent = Math.min(1.0F, Math.max(0.0F, (float) liveMana / 10.0F));

        //LogManager.debug("Cache Diagnostic -> Cogs: {" + liveCogs + "}");
        //LogManager.debug("Cache Diagnostic -> Food: {" + liveFood + "}");
        //LogManager.debug("Cache Diagnostic -> Mana: {" + liveMana + "}");

        // --- COLUMN 1: LEFT SIDE (POWER & AIRSHIPS) ---
        int col1X = this.x + 8;

        this.powerHeader.render(graphics, col1X, this.y + 6, 0.0F);
        graphics.drawString(this.font, Component.literal("Power: " + factionSize + " / " + totalPlayerCount), col1X, this.y + 18, FactionPalette.getBarColor("text_pink"), false);
        this.powerBar.render(graphics, col1X, this.y + 28, powerPercent);

        this.airshipsLabel.render(graphics, col1X, this.y + 44, 0.0F);
        this.airshipsBar.render(graphics, col1X, this.y + 54, shipsPercent);

        int villagerCount = (faction != null) ? faction.villagerCount : 0;
        int controlledVillages = (faction != null) ? faction.controlledVillages : 0;

        this.territoryLabel.render(graphics, col1X, this.y + 66, 0.0F);
        graphics.drawString(this.font, Component.literal("Villagers: " + villagerCount), col1X, this.y + 78, FactionPalette.getBarColor("text_pink"), false);
        graphics.drawString(this.font, Component.literal("Villages: " + controlledVillages), col1X, this.y + 88, FactionPalette.getBarColor("text_pink"), false);

        // --- COLUMN 2: RIGHT SIDE (STATISTICS & STORAGE) ---
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