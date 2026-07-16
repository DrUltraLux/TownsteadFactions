package com.drultralux.townsteadfactions.client.screen.widget;

import com.drultralux.townsteadfactions.client.ClientFactionCache;
import com.drultralux.townsteadfactions.client.ClientFactionCache.ClientFactionData;
import dev.marie.MariesLib.client.GuiValueRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Map;

/**
 * A draggable dashboard interface component showing all discovered server factions.
 * Displays faction names, leadership identities, and member population densities.
 */
public class GlobalFactionsWidget extends DraggableWidget {
    private final Font font;

    /**
     * Allocates specific bounding dimensions to build the world factions overview frame.
     *
     * @param x the initial horizontal rendering coordinate tracking vector
     * @param y the initial vertical rendering coordinate tracking vector
     */
    public GlobalFactionsWidget(int x, int y) {
        super(x, y, 180, 120);
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
            graphics.drawString(this.font, "§6§lWORLD FACTIONS (MIN)", this.x + 6, this.y + 3, 0xFFFFFF, false);
            return;
        }

        int backgroundColor = this.isDragging ? 0xAA3B3B3B : 0xAA222222;
        graphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, backgroundColor);
        graphics.renderOutline(this.x, this.y, this.width, this.height, 0xFF666666);

        String minBtnText = isMinimizeButtonHovered(mouseX, mouseY) ? "§e[-]" : "§7[-]";
        graphics.drawString(this.font, minBtnText, this.x + this.width - 16, this.y + 3, 0xFFFFFF, false);

        graphics.drawString(this.font, "§6§lDISCOVERED FACTIONS", this.x + 6, this.y + 6, 0xFFFFFF, false);

        Map<String, ClientFactionData> globalFactions = ClientFactionCache.getCachedFactions();
        String currentFactionId = ClientFactionCache.getAssignedFactionId();

        if (globalFactions.isEmpty()) {
            graphics.drawString(this.font, "§7No world profiles cached.", this.x + 8, this.y + 24, 0xAAAAAA, false);
            return;
        }

        int textOffset = 24;
        int maxRows = 6;
        int rowCounter = 0;

        for (ClientFactionData faction : globalFactions.values()) {
            boolean isOwnFaction = faction.id.equalsIgnoreCase(currentFactionId);

            // Apply bright visual indicators if the row is the player's own group
            String rowString = isOwnFaction
                    ? "§a✔ §e" + faction.name + " §7(" + faction.roster.size() + ")"
                    : "§7• §f" + faction.name + " §7(" + faction.roster.size() + ")";

            graphics.drawString(this.font, rowString, this.x + 8, this.y + textOffset, 0xFFFFFF, false);
            textOffset += 14;
            rowCounter++;

            if (rowCounter >= maxRows) {
                int hiddenCount = globalFactions.size() - maxRows;
                if (hiddenCount > 0) {
                    graphics.drawString(this.font, "§8+ " + hiddenCount + " additional groups...", this.x + 8, this.y + textOffset, 0x666666, false);
                }
                break;
            }
        }
    }
}