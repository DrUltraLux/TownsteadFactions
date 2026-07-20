package com.drultralux.townsteadfactions.client.screen.widget;

import com.drultralux.townsteadfactions.client.ClientFactionCache;
import com.drultralux.townsteadfactions.client.ClientFactionCache.ClientFactionData;
import com.drultralux.townsteadfactions.client.ClientFactionCache.RosterEntry;
import com.drultralux.townsteadfactions.client.screen.FactionPalette;
import com.drultralux.townsteadfactions.factions.FactionTitle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A draggable, scrollable widget that lists every member of the player's
 * currently assigned faction, sorted by rank (Leader, then Monarch/
 * Noble/Knight/Soldier if applicable, then everyone else) so anyone who
 * outranks the default tier sorts near the top of the scroll window
 * instead of depending on map iteration order.
 */
public class RosterDisplayWidget extends DraggableWidget {

    /** How many roster lines are visible at once. */
    private static final int VISIBLE_LINES = 10;

    /**
     * Rank tier for each known title, lower sorts first. Anything not in
     * this map (Member, Villager, Commoner, or an unrecognized
     * self-assigned cosmetic string) falls through to the default tier
     * via {@link #rankOf}. Leader isn't listed here — it's handled
     * separately via {@link RosterEntry#isLeader()} so it always wins
     * regardless of what the title string happens to say.
     */
    private static final Map<String, Integer> RANK_ORDER = Map.of(
            FactionTitle.MONARCH.getDisplayName(), 1,
            FactionTitle.NOBLE.getDisplayName(), 2,
            FactionTitle.KNIGHT.getDisplayName(), 3,
            FactionTitle.SOLDIER.getDisplayName(), 4
    );

    /** The rank tier used for anything not explicitly listed in {@link #RANK_ORDER}. */
    private static final int DEFAULT_RANK = 5;

    /** The font used to draw the header and roster entries. */
    private final Font font;

    /**
     * How many entries are currently skipped from the top of the
     * rank-sorted combined list. Persists across cache updates, same as
     * the activity log's scroll offset, so incoming updates don't
     * visually jump the player's scroll position.
     */
    private int scrollOffset = 0;

    /**
     * Creates the roster display widget at the given position with a
     * fixed default size.
     *
     * @param x the x position of the widget
     * @param y the y position of the widget
     */
    public RosterDisplayWidget(int x, int y) {
        super(x, y, 220, 90);
        this.font = Minecraft.getInstance().font;
    }

    /**
     * Renders the widget's minimized header, or its background, header,
     * and the current scrolled window of rank-sorted roster entries
     * (players in white, villagers in muted grey).
     *
     * @param graphics the graphics context to draw with
     * @param mouseX the current mouse x position
     * @param mouseY the current mouse y position
     * @param partialTicks the partial tick time, for frame interpolation
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (this.isMinimized) {
            renderMinimizedHeader(graphics, this.font, "FACTION ROSTER");
            return;
        }

        graphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, 0x66000000);
        graphics.renderOutline(this.x, this.y, this.width, this.height, 0xFF444444);

        renderMinimizeButton(graphics, this.font);

        String rosterHeader = "FACTION ROSTER";
        graphics.drawString(this.font, Component.literal(rosterHeader), this.x + 6, this.y + 5, FactionPalette.getBarColor("text_gold"), false);
        graphics.fill(this.x + 4, this.y + 14, this.x + this.width - 4, this.y + 15, 0xFF333333);

        List<CombinedEntry> combined = getCombinedEntries();
        combined.sort(Comparator.comparingInt(RosterDisplayWidget::rankOf)
                .thenComparing(e -> e.entry().name(), String.CASE_INSENSITIVE_ORDER));

        if (combined.isEmpty()) {
            graphics.drawString(this.font, Component.literal("§cNo members cached."), this.x + 10, this.y + 18, 0xFFFFFF, false);
            return;
        }

        int maxOffset = Math.max(0, combined.size() - VISIBLE_LINES);
        this.scrollOffset = Math.min(this.scrollOffset, maxOffset);

        int currentYOffset = this.y + 18;
        int endIndex = Math.min(combined.size(), this.scrollOffset + VISIBLE_LINES);
        for (int i = this.scrollOffset; i < endIndex; i++) {
            CombinedEntry entry = combined.get(i);
            String line = (entry.isPlayer() ? "§7• §f" : "§7• §7") + truncateName(entry.entry().name(), 16) + " §7(" + entry.entry().root() + ") §e" + entry.entry().title();
            graphics.drawString(this.font, Component.literal(line), this.x + 10, currentYOffset, FactionPalette.getBarColor("ships"), false);
            currentYOffset += 10;
            if (currentYOffset >= this.y + this.height - 8) break;
        }
    }

    /**
     * Scrolls the roster list when the mouse wheel is used over this
     * widget, clamped so it never scrolls past the last entry.
     *
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     * @param scrollX the horizontal scroll amount, unused
     * @param scrollY the vertical scroll amount; positive scrolls toward the top
     * @return {@code true} if the scroll was handled by this widget
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isMinimized || !isHovered((int) mouseX, (int) mouseY)) return false;

        List<CombinedEntry> combined = getCombinedEntries();
        int maxOffset = Math.max(0, combined.size() - VISIBLE_LINES);
        this.scrollOffset = Math.max(0, Math.min(maxOffset, this.scrollOffset - (int) Math.signum(scrollY)));
        return true;
    }

    /**
     * Computes an entry's sort tier: Leader always wins outright (checked
     * via the real {@code isLeader} flag, not the title string, so it's
     * correct even if display text is ever localized or customized);
     * otherwise Monarch/Noble/Knight/Soldier sort by {@link #RANK_ORDER};
     * everything else (Member, Villager, Commoner, or an unrecognized
     * cosmetic title) shares {@link #DEFAULT_RANK}.
     *
     * @param combinedEntry the entry to rank
     * @return the sort tier, lower sorts first
     */
    private static int rankOf(CombinedEntry combinedEntry) {
        if (combinedEntry.entry().isLeader()) return 0;
        return RANK_ORDER.getOrDefault(combinedEntry.entry().title(), DEFAULT_RANK);
    }

    /**
     * Truncates a name to a maximum length, appending "…" if it was cut
     * short, so an unusually long name (player or villager) can never
     * push the rest of a roster line past the widget's edge.
     *
     * @param name the name to truncate
     * @param maxLength the maximum length, including the ellipsis if added
     * @return the truncated name
     */
    private static String truncateName(String name, int maxLength) {
        if (name == null) return "";
        return (name.length() > maxLength) ? name.substring(0, maxLength - 1) + "…" : name;
    }

    /**
     * Builds the combined, ordered list of roster entries for the
     * player's current faction: players first, then villagers.
     *
     * @return the combined entries, or an empty list if no faction is assigned
     */
    private List<CombinedEntry> getCombinedEntries() {
        List<CombinedEntry> result = new ArrayList<>();
        String activeId = ClientFactionCache.getAssignedFactionId();
        ClientFactionData factionData = ClientFactionCache.getCachedFactions().get(activeId);
        if (factionData == null) return result;

        for (Map.Entry<UUID, RosterEntry> entry : factionData.roster.entrySet()) {
            result.add(new CombinedEntry(entry.getValue(), true));
        }
        for (Map.Entry<UUID, RosterEntry> entry : factionData.villagerRoster.entrySet()) {
            result.add(new CombinedEntry(entry.getValue(), false));
        }
        return result;
    }

    /**
     * A single roster entry paired with whether it's a player or villager,
     * for combined-list ordering and display styling.
     *
     * @param entry the underlying roster entry data
     * @param isPlayer whether this entry is a player (as opposed to a villager)
     */
    private record CombinedEntry(RosterEntry entry, boolean isPlayer) {}
}