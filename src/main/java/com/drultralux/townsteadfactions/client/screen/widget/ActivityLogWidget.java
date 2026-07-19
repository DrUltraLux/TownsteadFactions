package com.drultralux.townsteadfactions.client.screen.widget;

import com.drultralux.townsteadfactions.client.ClientFactionCache;
import com.drultralux.townsteadfactions.client.ClientFactionCache.ClientFactionData;
import com.drultralux.townsteadfactions.client.screen.FactionPalette;
import com.drultralux.townsteadfactions.factions.ActivityLogEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * A draggable widget showing the player's faction's activity log: the
 * most recent entries by default, with mouse-wheel scrolling to look
 * further back through history. Requests older entries from the server
 * on demand as the player scrolls near the edge of what's currently
 * cached.
 */
public class ActivityLogWidget extends DraggableWidget {

    /** How many log lines are visible at once. */
    private static final int VISIBLE_LINES = 10;

    /** How close to the end of cached history (in entries) triggers a request for more. */
    private static final int PREFETCH_THRESHOLD = 3;

    /** The font used to draw the header and log lines. */
    private final Font font;

    /**
     * How many entries are currently skipped from the top (newest) of the
     * cached list. Deliberately persists across cache updates rather than
     * being reset — so new entries arriving don't visually jump the
     * player's scroll position; the window simply reflects whatever now
     * sits at this offset.
     */
    private int scrollOffset = 0;

    /**
     * Creates the activity log widget at the given position with a fixed
     * default size.
     *
     * @param x the x position of the widget
     * @param y the y position of the widget
     */
    public ActivityLogWidget(int x, int y) {
        super(x, y, 210, 112);
        this.font = Minecraft.getInstance().font;
    }

    /**
     * Renders the widget's background, header, and the current window of
     * visible log lines for the player's faction, unless the widget is
     * minimized.
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

        String logHeader = "⚙ FACTION LOG";
        graphics.drawString(this.font, Component.literal(logHeader), this.x + 6, this.y + 5, FactionPalette.getBarColor("text_gold"), false);
        graphics.fill(this.x + 4, this.y + 14, this.x + this.width - 4, this.y + 15, 0xFF333333);

        String activeId = ClientFactionCache.getAssignedFactionId();
        ClientFactionData factionData = ClientFactionCache.getCachedFactions().get(activeId);
        List<ActivityLogEntry> entries = (factionData != null) ? factionData.activityLog : List.of();

        if (entries.isEmpty()) {
            graphics.drawString(this.font, Component.literal("§7No activity recorded yet."), this.x + 8, this.y + 18, 0xFFAAAAAA, false);
            return;
        }

        int maxOffset = Math.max(0, entries.size() - VISIBLE_LINES);
        this.scrollOffset = Math.min(this.scrollOffset, maxOffset);

        int currentYOffset = this.y + 18;
        int endIndex = Math.min(entries.size(), this.scrollOffset + VISIBLE_LINES);
        for (int i = this.scrollOffset; i < endIndex; i++) {
            String line = "§7• §f" + entries.get(i).message();
            graphics.drawString(this.font, Component.literal(line), this.x + 8, currentYOffset, FactionPalette.getBarColor("text_blue"), false);
            currentYOffset += 9;
            if (currentYOffset >= this.y + this.height - 6) break;
        }

        // Prefetch older history once the player scrolls near the end of what's cached.
        if (factionData != null && factionData.hasMoreLogHistory && entries.size() - this.scrollOffset <= VISIBLE_LINES + PREFETCH_THRESHOLD) {
            ClientFactionCache.requestMoreActivityLog(activeId);
        }
    }

    /**
     * Scrolls the visible log window when the mouse wheel is used over
     * this widget, clamped so it never scrolls past the oldest
     * currently-cached entry.
     *
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     * @param scrollX the horizontal scroll amount, unused
     * @param scrollY the vertical scroll amount; positive scrolls toward newer entries
     * @return {@code true} if the scroll was handled by this widget
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isMinimized || !isHovered((int) mouseX, (int) mouseY)) return false;

        String activeId = ClientFactionCache.getAssignedFactionId();
        ClientFactionData factionData = ClientFactionCache.getCachedFactions().get(activeId);
        List<ActivityLogEntry> entries = (factionData != null) ? factionData.activityLog : List.of();

        int maxOffset = Math.max(0, entries.size() - VISIBLE_LINES);
        this.scrollOffset = Math.max(0, Math.min(maxOffset, this.scrollOffset - (int) Math.signum(scrollY)));
        return true;
    }
}