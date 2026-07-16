package com.drultralux.townsteadfactions.client.screen.widget;

import com.drultralux.townsteadfactions.client.ClientFactionCache;
import com.drultralux.townsteadfactions.client.ClientFactionCache.ClientFactionData;
import dev.marie.MariesLib.client.GuiValueRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Map;
import java.util.UUID;

/**
 * A draggable dashboard interface component showing faction membership tables.
 */
public class RosterDisplayWidget extends DraggableWidget {
    private final Font font;

    /**
     * Builds out our active roster display element utilizing fixed size dimensions.
     *
     * @param x the initial horizontal position vector coordinate
     * @param y the initial vertical position vector coordinate
     */
    public RosterDisplayWidget(int x, int y) {
        super(x, y, 160, 120);
        this.font = Minecraft.getInstance().font;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, GuiValueRenderer barRenderer) {
        int backgroundColor = this.isDragging ? 0xAA555555 : 0xAA2A2A2A;
        graphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, backgroundColor);
        graphics.renderOutline(this.x, this.y, this.width, this.height, 0xFF777777);

        graphics.drawString(this.font, "§6§lFACTION ROSTER", this.x + 6, this.y + 6, 0xFFFFFF, false);

        String activeId = ClientFactionCache.getAssignedFactionId();
        ClientFactionData faction = ClientFactionCache.getCachedFactions().get(activeId);

        if (faction == null || faction.roster.isEmpty()) {
            graphics.drawString(this.font, "§7No members found.", this.x + 8, this.y + 24, 0xAAAAAA, false);
            return;
        }

        int textOffset = 24;
        int maxVisibleRows = 7;
        int rowCounter = 0;

        for (Map.Entry<UUID, String> entry : faction.roster.entrySet()) {
            String shortUuid = entry.getKey().toString().substring(0, 8) + "...";
            String titleRole = entry.getValue();

            String formatPattern = titleRole.equalsIgnoreCase("LEADER") || titleRole.equalsIgnoreCase("MONARCH")
                    ? "§e★ " + shortUuid + " (§6" + titleRole + "§e)"
                    : "§7• " + shortUuid + " (§b" + titleRole + "§7)";

            graphics.drawString(this.font, formatPattern, this.x + 8, this.y + textOffset, 0xFFFFFF, false);
            textOffset += 13;
            rowCounter++;

            if (rowCounter >= maxVisibleRows) {
                graphics.drawString(this.font, "§8+ " + (faction.roster.size() - maxVisibleRows) + " more...", this.x + 8, this.y + textOffset, 0x888888, false);
                break;
            }
        }
    }
}