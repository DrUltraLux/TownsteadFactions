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
 * A draggable widget that lists the display names of members in the
 * player's currently assigned faction.
 */
public class RosterDisplayWidget extends DraggableWidget {

    /** The font used to draw the header and roster entries. */
    private final Font font;

    /**
     * Creates the roster display widget at the given position with a fixed
     * default size.
     *
     * @param x the x position of the widget
     * @param y the y position of the widget
     */
    public RosterDisplayWidget(int x, int y) {
        super(x, y, 150, 75);
        this.font = Minecraft.getInstance().font;
    }

    /**
     * Renders the widget's background, header, and the list of cached
     * roster member names for the player's current faction, unless the
     * widget is minimized.
     *
     * @param graphics the graphics context to draw with
     * @param mouseX the current mouse x position
     * @param mouseY the current mouse y position
     * @param partialTicks the partial tick time, for frame interpolation
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, 0x66000000);
        graphics.renderOutline(this.x, this.y, this.width, this.height, 0xFF444444);

        if (this.isMinimized) return;

        String rosterHeader = "FACTION ROSTER";
        graphics.drawString(this.font, Component.literal(rosterHeader), this.x + 6, this.y + 5, FactionPalette.getBarColor("text_gold"), false);
        graphics.fill(this.x + 4, this.y + 14, this.x + this.width - 4, this.y + 15, 0xFF333333);

        String currentFactionId = ClientFactionCache.getAssignedFactionId();
        Map<String, ClientFactionData> cachedMap = ClientFactionCache.getCachedFactions();
        ClientFactionData factionData = cachedMap.get(currentFactionId);

        int currentYOffset = this.y + 18;
        if (factionData != null && factionData.roster != null && !factionData.roster.isEmpty()) {
            for (String displayName : factionData.roster.values()) {
                if (displayName == null || displayName.trim().isEmpty()) continue;
                graphics.drawString(this.font, Component.literal("§7• §f" + displayName), this.x + 10, currentYOffset, FactionPalette.getBarColor("ships"), false);
                currentYOffset += 10;
                if (currentYOffset >= this.y + this.height - 8) break;
            }
        } else {
            graphics.drawString(this.font, Component.literal("§cNo members cached."), this.x + 10, currentYOffset, 0xFFFFFF, false);
        }
    }
}