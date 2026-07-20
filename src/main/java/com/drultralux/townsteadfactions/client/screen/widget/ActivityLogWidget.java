package com.drultralux.townsteadfactions.client.screen.widget;

import com.drultralux.townsteadfactions.client.ClientFactionCache;
import com.drultralux.townsteadfactions.client.ClientFactionCache.ClientFactionData;
import com.drultralux.townsteadfactions.client.screen.FactionPalette;
import com.drultralux.townsteadfactions.factions.ActivityLogEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * A draggable widget showing the player's faction's activity log: the
 * most recent entries by default, with mouse-wheel scrolling to look
 * further back through history. Long entries wrap onto multiple lines
 * (via vanilla's own text-wrapping, the same mechanism used for books and
 * signs) rather than ever requiring the widget to grow wider. Requests
 * older entries from the server on demand as the player scrolls near the
 * edge of what's currently cached.
 */
public class ActivityLogWidget extends DraggableWidget {

    /** How close to the end of cached history (in entries) triggers a request for more. */
    private static final int PREFETCH_THRESHOLD = 3;

    /** The height, in pixels, of a single rendered (wrapped) line. */
    private static final int LINE_HEIGHT = 9;

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
        super(x, y, 230, 112);
        this.font = Minecraft.getInstance().font;
    }

    /**
     * Renders the widget's background, header, and as many (wrapped)
     * lines of log entries as fit in the available height, starting from
     * the current scroll offset, unless the widget is minimized.
     *
     * @param graphics the graphics context to draw with
     * @param mouseX the current mouse x position
     * @param mouseY the current mouse y position
     * @param partialTicks the partial tick time, for frame interpolation
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (this.isMinimized) {
            renderMinimizedHeader(graphics, this.font, "FACTION LOG");
            return;
        }

        graphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, 0x66000000);
        graphics.renderOutline(this.x, this.y, this.width, this.height, 0xFF444444);

        renderMinimizeButton(graphics, this.font);

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

        int maxOffset = Math.max(0, entries.size() - 1);
        this.scrollOffset = Math.min(this.scrollOffset, maxOffset);

        int textMaxWidth = this.width - 16;
        int currentY = this.y + 18;
        int bottomBound = this.y + this.height - 6;

        outer:
        for (int i = this.scrollOffset; i < entries.size(); i++) {
            String line = "§7• §f" + entries.get(i).message();
            List<FormattedCharSequence> wrapped = this.font.split(Component.literal(line), textMaxWidth);
            for (FormattedCharSequence wrappedLine : wrapped) {
                if (currentY >= bottomBound) break outer;
                graphics.drawString(this.font, wrappedLine, this.x + 8, currentY, FactionPalette.getBarColor("text_blue"), false);
                currentY += LINE_HEIGHT;
            }
        }

        // Prefetch older history once the player scrolls near the end of what's cached.
        if (factionData != null && factionData.hasMoreLogHistory && entries.size() - this.scrollOffset <= PREFETCH_THRESHOLD + 3) {
            ClientFactionCache.requestMoreActivityLog(activeId);
        }
    }

    /**
     * Scrolls the visible log window when the mouse wheel is used over
     * this widget, one entry at a time, clamped so it never scrolls past
     * the last cached entry.
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

        int maxOffset = Math.max(0, entries.size() - 1);
        this.scrollOffset = Math.max(0, Math.min(maxOffset, this.scrollOffset - (int) Math.signum(scrollY)));
        return true;
    }
}