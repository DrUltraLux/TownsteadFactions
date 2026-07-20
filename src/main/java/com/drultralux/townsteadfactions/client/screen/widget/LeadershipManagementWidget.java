package com.drultralux.townsteadfactions.client.screen.widget;

import com.drultralux.townsteadfactions.client.ClientFactionCache;
import com.drultralux.townsteadfactions.client.ClientFactionCache.ClientFactionData;
import com.drultralux.townsteadfactions.client.ClientFactionCache.RosterEntry;
import com.drultralux.townsteadfactions.client.screen.FactionPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A draggable widget holding a faction's leadership-only controls: a
 * "Resign Leadership" button, and (non-Capitals only) a scrollable,
 * click-to-select list of eligible members to nominate for leadership,
 * greyed out once the faction's leader capacity is reached. Only ever
 * placed on the conditional Leadership tab, which itself only exists
 * while the viewer is currently a leader — so no separate eligibility
 * gating is needed within this widget for the resign action.
 */
public class LeadershipManagementWidget extends DraggableWidget {

    /** The y offset, relative to the widget's top, where the scrollable nominate list begins. */
    private static final int LIST_AREA_TOP = 46;

    /** The height, in pixels, of a single nominate row. */
    private static final int ROW_HEIGHT = 9;

    /** The font used to draw all text in this widget. */
    private final Font font;

    /** The bounds of each currently-rendered nominate row, for click hit-testing, refreshed every render. */
    private final List<NominateRow> renderedRows = new ArrayList<>();

    /**
     * How far the nominate list is currently scrolled down, measured in
     * entries skipped from the top. Persists across cache updates, same
     * as the other scrollable widgets, so incoming updates don't visually
     * jump the player's scroll position.
     */
    private int scrollOffset = 0;

    /**
     * A single rendered nominate row's clickable bounds and target.
     *
     * @param uuid the member this row would nominate
     * @param top the row's top y position
     * @param bottom the row's bottom y position
     */
    private record NominateRow(UUID uuid, int top, int bottom) {}

    /**
     * A single nominate candidate paired with whether they're a player or
     * villager, for combined-list ordering and display styling.
     *
     * @param entry the underlying roster entry data
     * @param isPlayer whether this entry is a player (as opposed to a villager)
     * @param uuid the candidate's UUID
     */
    private record CombinedEntry(RosterEntry entry, boolean isPlayer, UUID uuid) {}

    /**
     * Creates the leadership management widget at the given position with
     * a fixed default size.
     *
     * @param x the x position of the widget
     * @param y the y position of the widget
     */
    public LeadershipManagementWidget(int x, int y) {
        super(x, y, 200, 150);
        this.font = Minecraft.getInstance().font;
    }

    /**
     * Renders the widget's background, header, resign button, and — if
     * Capitals isn't functionally present — the leader-capacity counter
     * and a scrollable, clickable list of members eligible to be
     * nominated for leadership.
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

        this.renderedRows.clear();

        if (this.isMinimized) {
            renderMinimizedHeader(graphics, this.font, "LEADERSHIP");
            return;
        }

        renderMinimizeButton(graphics, this.font);

        graphics.drawString(this.font, Component.literal("LEADERSHIP"), this.x + 6, this.y + 5, FactionPalette.getBarColor("text_gold"), false);
        graphics.fill(this.x + 4, this.y + 14, this.x + this.width - 4, this.y + 15, 0xFF333333);

        graphics.fill(this.x + 6, this.y + 18, this.x + this.width - 6, this.y + 30, 0xFF6E3A3A);
        graphics.renderOutline(this.x + 6, this.y + 18, this.width - 12, 12, 0xFF555555);
        String resignLabel = "Resign Leadership";
        int resignLabelWidth = this.font.width(resignLabel);
        graphics.drawString(this.font, Component.literal(resignLabel), this.x + (this.width - resignLabelWidth) / 2, this.y + 21, 0xFFFFCCCC, false);

        ClientFactionData data = ClientFactionCache.getCachedFactions().get(ClientFactionCache.getAssignedFactionId());
        int currentY = this.y + 36;

        if (data != null && data.capitalsFunctional) {
            graphics.drawString(this.font, Component.literal("§7Leadership is managed"), this.x + 8, currentY, 0xFFAAAAAA, false);
            graphics.drawString(this.font, Component.literal("§7automatically via Capitals."), this.x + 8, currentY + 9, 0xFFAAAAAA, false);
            return;
        }

        int totalParticipants = (data != null) ? data.roster.size() + data.villagerRoster.size() : 0;
        int currentLeaders = 0;
        if (data != null) {
            for (RosterEntry entry : data.roster.values()) {
                if (entry.isLeader()) currentLeaders++;
            }
            for (RosterEntry entry : data.villagerRoster.values()) {
                if (entry.isLeader()) currentLeaders++;
            }
        }
        int maxLeaders = Math.max(1, Math.round(totalParticipants / 10.0f));
        boolean canNominateMore = currentLeaders < maxLeaders;

        String nominateLabel = "Nominate: (" + currentLeaders + "/" + maxLeaders + ")";
        graphics.drawString(this.font, Component.literal(nominateLabel), this.x + 6, currentY, canNominateMore ? FactionPalette.getBarColor("text_blue") : 0xFF666666, false);

        if (!canNominateMore) {
            graphics.drawString(this.font, Component.literal("§7Leader capacity reached."), this.x + 10, currentY + 10, 0xFF888888, false);
            return;
        }

        List<CombinedEntry> candidates = getCandidates(data);
        int listAreaTop = this.y + LIST_AREA_TOP;
        int listAreaBottom = this.y + this.height - 4;

        if (candidates.isEmpty()) {
            graphics.drawString(this.font, Component.literal("§7No eligible members."), this.x + 10, listAreaTop, 0xFF888888, false);
            return;
        }

        int maxScroll = Math.max(0, candidates.size() - 1);
        this.scrollOffset = Math.min(this.scrollOffset, maxScroll);

        graphics.enableScissor(this.x + 4, listAreaTop, this.x + this.width - 4, listAreaBottom);

        int rowY = listAreaTop;
        for (int i = this.scrollOffset; i < candidates.size(); i++) {
            if (rowY >= listAreaBottom) break;

            CombinedEntry candidate = candidates.get(i);
            String line = (candidate.isPlayer() ? "§7• §f" : "§7• §7") + candidate.entry().name();
            graphics.drawString(this.font, Component.literal(line), this.x + 10, rowY, 0xFFDDDDDD, false);
            this.renderedRows.add(new NominateRow(candidate.uuid(), rowY, rowY + ROW_HEIGHT));
            rowY += ROW_HEIGHT;
        }

        graphics.disableScissor();
    }

    /**
     * Scrolls the nominate list when the mouse wheel is used over this
     * widget's list area, clamped so it never scrolls past the last
     * candidate.
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

        ClientFactionData data = ClientFactionCache.getCachedFactions().get(ClientFactionCache.getAssignedFactionId());
        List<CombinedEntry> candidates = getCandidates(data);
        int maxScroll = Math.max(0, candidates.size() - 1);
        this.scrollOffset = Math.max(0, Math.min(maxScroll, this.scrollOffset - (int) Math.signum(scrollY)));
        return true;
    }

    /**
     * Builds the combined, ordered list of nominate-eligible candidates
     * for the player's current faction: non-leader players first, then
     * non-leader villagers.
     *
     * @param data the faction's cached data, or {@code null} if unavailable
     * @return the eligible candidates
     */
    private List<CombinedEntry> getCandidates(ClientFactionData data) {
        List<CombinedEntry> result = new ArrayList<>();
        if (data == null) return result;

        java.util.Set<UUID> alreadyNominated = new java.util.HashSet<>();
        for (com.drultralux.townsteadfactions.client.ClientFactionCache.ClientVoteInfo vote : data.activeVotes) {
            if (vote.type() == com.drultralux.townsteadfactions.factions.voting.VoteType.ELECT) {
                alreadyNominated.add(vote.targetUUID());
            }
        }

        for (Map.Entry<UUID, RosterEntry> entry : data.roster.entrySet()) {
            if (!entry.getValue().isLeader() && !alreadyNominated.contains(entry.getKey())) {
                result.add(new CombinedEntry(entry.getValue(), true, entry.getKey()));
            }
        }
        for (Map.Entry<UUID, RosterEntry> entry : data.villagerRoster.entrySet()) {
            if (!entry.getValue().isLeader() && !alreadyNominated.contains(entry.getKey())) {
                result.add(new CombinedEntry(entry.getValue(), false, entry.getKey()));
            }
        }
        return result;
    }

    /**
     * Handles clicks: the "Resign Leadership" button, or a nominate row
     * (nominates that member for leadership).
     *
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     * @param button the mouse button pressed
     * @return {@code true} if the click was handled by this widget
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || this.isMinimized) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (isMinimizeButtonHovered((int) mouseX, (int) mouseY)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (mouseX >= this.x + 6 && mouseX <= this.x + this.width - 6 && mouseY >= this.y + 18 && mouseY <= this.y + 30) {
            ClientFactionCache.resignLeadership();
            return true;
        }

        for (NominateRow row : this.renderedRows) {
            if (mouseY >= row.top() && mouseY < row.bottom() && mouseX >= this.x + 10 && mouseX <= this.x + this.width - 4) {
                ClientFactionCache.nominateForLeadership(row.uuid());
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}