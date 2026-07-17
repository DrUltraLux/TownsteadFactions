package com.drultralux.townsteadfactions.client.screen.widget;

import com.drultralux.townsteadfactions.client.ClientFactionCache;
import com.drultralux.townsteadfactions.client.ClientFactionCache.ClientFactionData;
import dev.marie.MariesLib.client.GuiValueRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.util.Map;

/**
 * A draggable dashboard interface component showing all discovered server factions.
 */
public class GlobalFactionsWidget extends DraggableWidget {
    private final Font font;

    public GlobalFactionsWidget(int x, int y) {
        super(x, y, 180, 120);
        this.font = Minecraft.getInstance().font;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, GuiValueRenderer barRenderer) {

        // Draw the main widget background panel bounding borders
        graphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, 0x99000000);
        graphics.renderOutline(this.x, this.y, this.x + this.width, this.y + this.height, 0xFF555555);

        String titleText = "§6=== Factions Overview ===";
        graphics.drawString(this.font, Component.literal(titleText), this.x + 8, this.y + 6, 0xFFFFFF, true);

        if (this.isMinimized) {
            String minimizedText = "§7[Minimized]";
            graphics.drawString(this.font, Component.literal(minimizedText), this.x + 12, this.y + 22, 0xFFFFFF, true);
            return;
        }

        Map<String, ClientFactionData> globalFactions = ClientFactionCache.getCachedFactions();
        String currentFactionId = ClientFactionCache.getAssignedFactionId();

        int currentYOffset = this.y + 22;

        if (globalFactions == null || globalFactions.isEmpty()) {
            graphics.drawString(this.font, Component.literal("§cNo factions loaded."), this.x + 12, currentYOffset, 0xFFFFFF, true);
            return;
        }
        for (ClientFactionData faction : globalFactions.values()) {
            if (faction == null || faction.id == null) continue;

            // Apply bright visual indicators if the row is the player's own group
            boolean isOwnFaction = faction.id.equalsIgnoreCase(currentFactionId);

            // Evaluates your true custom variables: faction.name and faction.roster.size() safely
            String rowString = isOwnFaction
                    ? "§a✔ §e" + faction.name + " §7(" + faction.roster.size() + ")"
                    : "§7• §f" + faction.name + " §7(" + faction.roster.size() + ")";

            graphics.drawString(this.font, Component.literal(rowString), this.x + 12, currentYOffset, 0xFFFFFF, true);

            // Advance the cursor downward for sequentially indexed rows
            currentYOffset += 12;

            // Prevent text strings from bleeding past the physical height bounding lines of the element frame box
            if (currentYOffset >= this.y + this.height - 10) {
                break;
            }
        }
    }
}