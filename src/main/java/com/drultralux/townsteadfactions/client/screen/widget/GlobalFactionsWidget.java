package com.drultralux.townsteadfactions.client.screen.widget;

import com.drultralux.townsteadfactions.client.ClientFactionCache;
import com.drultralux.townsteadfactions.client.ClientFactionCache.ClientFactionData;
import com.drultralux.townsteadfactions.client.screen.FactionPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.util.Map;

/**
 * A draggable widget listing every faction currently known to the client,
 * highlighting the player's own faction.
 */
public class GlobalFactionsWidget extends DraggableWidget {

    /** The font used to draw the title and faction rows. */
    private final Font font;

    /**
     * Creates the global factions widget at the given position with a
     * fixed default size.
     *
     * @param x the x position of the widget
     * @param y the y position of the widget
     */
    public GlobalFactionsWidget(int x, int y) {
        super(x, y, 180, 120);
        this.font = Minecraft.getInstance().font;
    }

    /**
     * Renders the widget's background, title, and either a minimized
     * summary or the full list of known factions.
     *
     * @param graphics the graphics context to draw with
     * @param mouseX the current mouse x position
     * @param mouseY the current mouse y position
     * @param partialTicks the partial tick time, for frame interpolation
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {

        graphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, 0x99000000);
        graphics.renderOutline(this.x, this.y, this.width, this.height, 0xFF555555);

        String titleText = "§6=== Factions ===";
        graphics.drawString(this.font, Component.literal(titleText), this.x + 8, this.y + 6, FactionPalette.getBarColor("text_gold"), true);

        if (this.isMinimized) {
            String minimizedText = "§7Factions [Min]";
            graphics.drawString(this.font, Component.literal(minimizedText), this.x + 12, this.y + 22, FactionPalette.getBarColor("text_gold"), true);
            return;
        }

        Map<String, ClientFactionData> globalFactions = ClientFactionCache.getCachedFactions();
        String currentFactionId = ClientFactionCache.getAssignedFactionId();

        int currentYOffset = this.y + 22;

        if (globalFactions == null || globalFactions.isEmpty()) {
            graphics.drawString(this.font, Component.literal("§cNo factions loaded."), this.x + 12, currentYOffset, FactionPalette.getBarColor("text_blue"), true);
            return;
        }
        for (ClientFactionData faction : globalFactions.values()) {
            if (faction == null || faction.id == null) continue;

            // Highlight the row if it represents the player's own faction
            boolean isOwnFaction = faction.id.equalsIgnoreCase(currentFactionId);

            int totalCount = faction.roster.size() + faction.villagerRoster.size();
            String rowString = isOwnFaction
                    ? "§a✔ §e" + faction.name + " §7(" + totalCount + ")"
                    : "§7• §f" + faction.name + " §7(" + totalCount + ")";

            graphics.drawString(this.font, Component.literal(rowString), this.x + 12, currentYOffset, FactionPalette.getBarColor("text_blue"), true);

            currentYOffset += 12;

            // Stop drawing rows once we'd overflow the widget's height
            if (currentYOffset >= this.y + this.height - 10) {
                break;
            }
        }
    }
}