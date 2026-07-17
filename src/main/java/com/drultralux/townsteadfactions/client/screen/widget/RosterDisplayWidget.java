package com.drultralux.townsteadfactions.client.screen.widget;

import com.drultralux.townsteadfactions.client.ClientFactionCache;
import com.drultralux.townsteadfactions.client.ClientFactionCache.ClientFactionData;
import dev.marie.MariesLib.client.GuiValueRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;

/**
 * Handles roster listings, extracting localized user display names from UUID tokens.
 */
public class RosterDisplayWidget extends DraggableWidget {
    private final Font font;

    public RosterDisplayWidget(int x, int y) {
        // Aligned bounding box footprint (150 width by 75 height)
        super(x, y, 150, 75);
        this.font = Minecraft.getInstance().font;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, 0x66000000);
        graphics.renderOutline(this.x, this.y, this.width, this.height, 0xFF444444);

        if (this.isMinimized) return;

        String rosterHeader = "FACTION ROSTER";
        graphics.drawString(this.font, Component.literal(rosterHeader), this.x + 6, this.y + 5, 0xFFA200, false);
        graphics.fill(this.x + 4, this.y + 14, this.x + this.width - 4, this.y + 15, 0xFF333333);

        String currentFactionId = ClientFactionCache.getAssignedFactionId();
        Map<String, ClientFactionData> cachedMap = ClientFactionCache.getCachedFactions();
        ClientFactionData factionData = cachedMap.get(currentFactionId);

        Minecraft mc = Minecraft.getInstance();
        int currentYOffset = this.y + 18;
        if (factionData != null && factionData.roster != null && !factionData.roster.isEmpty()) {
            for (UUID memberUUID : factionData.roster.keySet()) {
                if (memberUUID == null) continue;

                String displayName;
                Player levelPlayer = mc.level != null ? mc.level.getPlayerByUUID(memberUUID) : null;

                if (levelPlayer != null) {
                    displayName = levelPlayer.getName().getString();
                } else if (memberUUID.equals(mc.player.getUUID())) {
                    displayName = mc.player.getName().getString();
                } else {
                    // Fallback snippet format if the user record represents an offline player or dummy mock token
                    String rawString = memberUUID.toString();
                    displayName = "Player_" + rawString.substring(0, 5);
                }

                String lineEntry = "§7• §f" + displayName + " §a(Member)";
                graphics.drawString(this.font, Component.literal(lineEntry), this.x + 10, currentYOffset, 0xFFFFFF, false);

                currentYOffset += 10;

                // Prevent names from overflowing out of the bounding bottom frame box outlines
                if (currentYOffset >= this.y + this.height - 8) {
                    break;
                }
            }
        } else {
            graphics.drawString(this.font, Component.literal("§cNo members cached."), this.x + 10, currentYOffset, 0xFFFFFF, false);
        }
    }
}