package com.drultralux.townsteadfactions.client.screen.widget;

import com.drultralux.townsteadfactions.client.ClientFactionCache;
import com.drultralux.townsteadfactions.client.ClientFactionCache.ClientFactionData;
import com.drultralux.townsteadfactions.client.ClientFactionCache.ClientVoteInfo;
import com.drultralux.townsteadfactions.client.screen.FactionPalette;
import com.drultralux.townsteadfactions.factions.voting.VoteChoice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A draggable widget showing a "Request leadership position" self-
 * nomination button (non-Capitals only, greyed out and fully inert
 * otherwise) and a scrollable list of the faction's currently active
 * leadership votes. Each vote row can be independently expanded to
 * reveal Yes/No/Abstain buttons — but only if the viewer is currently
 * eligible to vote on it and hasn't already cast a choice; otherwise the
 * row stays a static single line with no expand affordance at all.
 */
public class VotingWidget extends DraggableWidget {

    /** The height, in pixels, of a single collapsed vote row. */
    private static final int ROW_HEIGHT_COLLAPSED = 10;

    /** The extra height, in pixels, an expanded row adds for its Yes/No/Abstain button line. */
    private static final int ROW_HEIGHT_EXPANDED_EXTRA = 11;

    /** The width, in pixels, of the internal vote-list scrollbar track. */
    private static final int SCROLLBAR_WIDTH = 5;

    /** The y offset, relative to the widget's top, where the vote list area begins. */
    private static final int LIST_AREA_TOP = 34;

    /** The font used to draw all text in this widget. */
    private final Font font;

    /** The vote IDs currently expanded to show their Yes/No/Abstain buttons. */
    private final Set<UUID> expandedVoteIds = new HashSet<>();

    /** How far the vote list is currently scrolled down, in pixels. */
    private int scrollOffset = 0;

    /** Whether the vote-list scrollbar thumb is currently being dragged. */
    private boolean isDraggingScrollbar = false;

    /** The mouse y position captured when a scrollbar drag begins. */
    private double scrollbarDragStartMouseY;

    /** This widget's {@link #scrollOffset} captured when a scrollbar drag begins. */
    private int scrollbarDragStartOffset;

    /**
     * Creates the voting widget at the given position with a fixed
     * default size.
     *
     * @param x the x position of the widget
     * @param y the y position of the widget
     */
    public VotingWidget(int x, int y) {
        super(x, y, 230, 130);
        this.font = Minecraft.getInstance().font;
    }

    /**
     * Renders the widget's background, header, "Request leadership
     * position" button, and the scrollable list of active votes, unless
     * minimized.
     *
     * @param graphics the graphics context to draw with
     * @param mouseX the current mouse x position
     * @param mouseY the current mouse y position
     * @param partialTicks the partial tick time, for frame interpolation
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (this.isMinimized) {
            renderMinimizedHeader(graphics, this.font, "VOTING");
            return;
        }

        graphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, 0x66000000);
        graphics.renderOutline(this.x, this.y, this.width, this.height, 0xFF444444);

        renderMinimizeButton(graphics, this.font);

        graphics.drawString(this.font, Component.literal("VOTING"), this.x + 6, this.y + 5, FactionPalette.getBarColor("text_gold"), false);
        graphics.fill(this.x + 4, this.y + 14, this.x + this.width - 4, this.y + 15, 0xFF333333);

        boolean requestButtonEnabled = isRequestLeadershipEnabled();
        int requestButtonColor = requestButtonEnabled ? 0xFF3A6E3A : 0xFF333333;
        int requestTextColor = requestButtonEnabled ? 0xFFCCFFCC : 0xFF777777;
        graphics.fill(this.x + 6, this.y + 18, this.x + this.width - 6, this.y + 30, requestButtonColor);
        graphics.renderOutline(this.x + 6, this.y + 18, this.width - 12, 12, 0xFF555555);
        String buttonLabel = "Request leadership position";
        int labelWidth = this.font.width(buttonLabel);
        graphics.drawString(this.font, Component.literal(buttonLabel), this.x + (this.width - labelWidth) / 2, this.y + 21, requestTextColor, false);

        List<ClientVoteInfo> votes = getActiveVotes();
        int listAreaTop = this.y + LIST_AREA_TOP;
        int listAreaBottom = this.y + this.height - 4;
        int listAreaHeight = listAreaBottom - listAreaTop;

        if (votes.isEmpty()) {
            graphics.drawString(this.font, Component.literal("§7No active votes."), this.x + 8, listAreaTop, 0xFFAAAAAA, false);
            return;
        }

        int totalContentHeight = 0;
        for (ClientVoteInfo vote : votes) {
            totalContentHeight += rowHeight(vote.voteId());
        }
        int maxScroll = Math.max(0, totalContentHeight - listAreaHeight);
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, maxScroll));

        graphics.enableScissor(this.x + 4, listAreaTop, this.x + this.width - 4 - SCROLLBAR_WIDTH - 2, listAreaBottom);

        int currentY = listAreaTop - this.scrollOffset;
        UUID localUUID = getLocalPlayerUUID();
        for (ClientVoteInfo vote : votes) {
            int thisRowHeight = rowHeight(vote.voteId());
            if (currentY + thisRowHeight >= listAreaTop && currentY <= listAreaBottom) {
                renderVoteRow(graphics, vote, currentY, localUUID);
            }
            currentY += thisRowHeight;
        }

        graphics.disableScissor();

        if (maxScroll > 0) {
            renderScrollbar(graphics, listAreaTop, listAreaBottom, maxScroll);
        }
    }

    /**
     * Renders a single vote's row: the always-visible summary line, plus
     * — if this vote is currently expanded — the Yes/No/Abstain button
     * line beneath it.
     *
     * @param graphics the graphics context to draw with
     * @param vote the vote to render
     * @param rowTop the y position this row starts at
     * @param localUUID the local player's UUID, or {@code null} if unavailable
     */
    private void renderVoteRow(GuiGraphics graphics, ClientVoteInfo vote, int rowTop, UUID localUUID) {
        long remainingMillis = vote.expiryTimestamp() - System.currentTimeMillis();
        String timeRemaining = formatTimeRemaining(remainingMillis);

        String summary = "§7• §f" + describeType(vote.type()) + ": §e" + vote.targetName() + " §7(" + vote.targetRoot() + ") §7" + timeRemaining +
                " §a" + vote.yesCount() + "§7/§c" + vote.noCount() + " §7(" + vote.totalEligibleVoters() + ")";
        graphics.drawString(this.font, Component.literal(summary), this.x + 8, rowTop, FactionPalette.getBarColor("ships"), false);

        if (this.expandedVoteIds.contains(vote.voteId())) {
            int buttonY = rowTop + ROW_HEIGHT_COLLAPSED;
            graphics.drawString(this.font, Component.literal("§a[ Yes ] §c[ No ] §7[ Abstain ]"), this.x + 14, buttonY, 0xFFFFFFFF, false);
        }
    }

    /**
     * Renders the vote-list scrollbar track and thumb.
     *
     * @param graphics the graphics context to draw with
     * @param listAreaTop the top of the scrollable list area
     * @param listAreaBottom the bottom of the scrollable list area
     * @param maxScroll the current maximum scroll offset
     */
    private void renderScrollbar(GuiGraphics graphics, int listAreaTop, int listAreaBottom, int maxScroll) {
        int trackX = this.x + this.width - 4 - SCROLLBAR_WIDTH;
        int trackHeight = listAreaBottom - listAreaTop;

        graphics.fill(trackX, listAreaTop, trackX + SCROLLBAR_WIDTH, listAreaBottom, 0xFF1A1A1A);

        int thumbHeight = Math.max(8, trackHeight * trackHeight / (trackHeight + maxScroll));
        int maxThumbTravel = trackHeight - thumbHeight;
        int thumbTop = listAreaTop + (maxScroll > 0 ? maxThumbTravel * this.scrollOffset / maxScroll : 0);

        graphics.fill(trackX, thumbTop, trackX + SCROLLBAR_WIDTH, thumbTop + thumbHeight, 0xFF888888);
    }

    /**
     * Handles clicks: the "Request leadership position" button (only if
     * enabled), the scrollbar thumb, a vote row's summary line (toggles
     * expand/collapse, only if currently actionable), or a Yes/No/Abstain
     * button within an expanded row (casts that choice).
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
            if (isRequestLeadershipEnabled()) {
                ClientFactionCache.requestLeadership();
            }
            return true;
        }

        List<ClientVoteInfo> votes = getActiveVotes();
        int listAreaTop = this.y + LIST_AREA_TOP;
        int listAreaBottom = this.y + this.height - 4;
        int maxScroll = computeMaxScroll(votes, listAreaBottom - listAreaTop);

        if (maxScroll > 0) {
            int trackX = this.x + this.width - 4 - SCROLLBAR_WIDTH;
            if (mouseX >= trackX && mouseX <= trackX + SCROLLBAR_WIDTH && mouseY >= listAreaTop && mouseY <= listAreaBottom) {
                this.isDraggingScrollbar = true;
                this.scrollbarDragStartMouseY = mouseY;
                this.scrollbarDragStartOffset = this.scrollOffset;
                return true;
            }
        }

        UUID localUUID = getLocalPlayerUUID();
        int currentY = listAreaTop - this.scrollOffset;
        for (ClientVoteInfo vote : votes) {
            int thisRowHeight = rowHeight(vote.voteId());
            boolean expanded = this.expandedVoteIds.contains(vote.voteId());
            boolean actionable = localUUID != null && vote.eligibleUUIDs().contains(localUUID) && !vote.votedUUIDs().contains(localUUID);

            if (mouseY >= currentY && mouseY < currentY + ROW_HEIGHT_COLLAPSED && mouseY >= listAreaTop && mouseY <= listAreaBottom) {
                if (actionable) {
                    if (expanded) {
                        this.expandedVoteIds.remove(vote.voteId());
                    } else {
                        this.expandedVoteIds.add(vote.voteId());
                    }
                }
                return true;
            }

            if (expanded && mouseY >= currentY + ROW_HEIGHT_COLLAPSED && mouseY < currentY + thisRowHeight && mouseY >= listAreaTop && mouseY <= listAreaBottom) {
                if (actionable) {
                    handleChoiceClick(mouseX, vote);
                }
                return true;
            }

            currentY += thisRowHeight;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Determines which of the Yes/No/Abstain buttons a click within an
     * expanded row's button line landed on, and casts that choice.
     *
     * @param mouseX the mouse x position
     * @param vote the vote being cast on
     */
    private void handleChoiceClick(double mouseX, ClientVoteInfo vote) {
        int baseX = this.x + 14;
        int yesEnd = baseX + this.font.width("§a[ Yes ] ");
        int noEnd = yesEnd + this.font.width("§c[ No ] ");

        VoteChoice choice;
        if (mouseX < yesEnd) {
            choice = VoteChoice.YES;
        } else if (mouseX < noEnd) {
            choice = VoteChoice.NO;
        } else {
            choice = VoteChoice.ABSTAIN;
        }

        ClientFactionCache.castVote(vote.voteId(), choice);
        this.expandedVoteIds.remove(vote.voteId());
    }

    /**
     * Handles the scrollbar drag.
     *
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     * @param button the mouse button held
     * @param dragX the horizontal drag delta
     * @param dragY the vertical drag delta
     */
    @Override
    public void mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isDraggingScrollbar) {
            List<ClientVoteInfo> votes = getActiveVotes();
            int listAreaHeight = (this.y + this.height - 4) - (this.y + LIST_AREA_TOP);
            int maxScroll = computeMaxScroll(votes, listAreaHeight);

            if (maxScroll > 0 && listAreaHeight > 0) {
                double delta = mouseY - this.scrollbarDragStartMouseY;
                int scrollDelta = (int) (delta * maxScroll / listAreaHeight);
                this.scrollOffset = Math.max(0, Math.min(maxScroll, this.scrollbarDragStartOffset + scrollDelta));
            }
            return;
        }
        super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    /**
     * Ends a scrollbar drag, if one was in progress.
     *
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     * @param button the mouse button released
     */
    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        this.isDraggingScrollbar = false;
        super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * Scrolls the vote list when the mouse wheel is used over this
     * widget's list area.
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

        List<ClientVoteInfo> votes = getActiveVotes();
        int listAreaHeight = (this.y + this.height - 4) - (this.y + LIST_AREA_TOP);
        int maxScroll = computeMaxScroll(votes, listAreaHeight);

        this.scrollOffset = Math.max(0, Math.min(maxScroll, this.scrollOffset - (int) (scrollY * 12)));
        return true;
    }

    /**
     * Computes the maximum scroll offset for the current vote list.
     *
     * @param votes the current votes
     * @param listAreaHeight the visible list area's height
     * @return the maximum scroll offset, in pixels
     */
    private int computeMaxScroll(List<ClientVoteInfo> votes, int listAreaHeight) {
        int total = 0;
        for (ClientVoteInfo vote : votes) {
            total += rowHeight(vote.voteId());
        }
        return Math.max(0, total - listAreaHeight);
    }

    /**
     * Returns a vote row's current height: taller if currently expanded.
     *
     * @param voteId the vote to measure
     * @return the row's height, in pixels
     */
    private int rowHeight(UUID voteId) {
        return this.expandedVoteIds.contains(voteId) ? ROW_HEIGHT_COLLAPSED + ROW_HEIGHT_EXPANDED_EXTRA : ROW_HEIGHT_COLLAPSED;
    }

    /**
     * Checks whether the "Request leadership position" button is
     * currently enabled: Capitals must not be functionally present, the
     * local player must not already be a leader, and they mustn't already
     * have an identical self-nomination vote active.
     *
     * @return {@code true} if the button is currently clickable
     */
    private boolean isRequestLeadershipEnabled() {
        ClientFactionData data = ClientFactionCache.getCachedFactions().get(ClientFactionCache.getAssignedFactionId());
        if (data == null || data.capitalsFunctional) return false;
        if (ClientFactionCache.isLocalPlayerLeader()) return false;

        UUID localUUID = getLocalPlayerUUID();
        if (localUUID == null) return false;

        for (ClientVoteInfo vote : data.activeVotes) {
            if (vote.type() == com.drultralux.townsteadfactions.factions.voting.VoteType.ELECT && vote.targetUUID().equals(localUUID)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the currently assigned faction's active votes.
     *
     * @return the active votes, or an empty list if no faction is assigned
     */
    private List<ClientVoteInfo> getActiveVotes() {
        ClientFactionData data = ClientFactionCache.getCachedFactions().get(ClientFactionCache.getAssignedFactionId());
        return (data != null) ? data.activeVotes : List.of();
    }

    /**
     * Returns the local player's UUID.
     *
     * @return the local player's UUID, or {@code null} if unavailable
     */
    private UUID getLocalPlayerUUID() {
        var player = Minecraft.getInstance().player;
        return (player != null) ? player.getUUID() : null;
    }

    /**
     * Produces a short, human-readable name for a vote type.
     *
     * @param type the vote type to describe
     * @return the description
     */
    private String describeType(com.drultralux.townsteadfactions.factions.voting.VoteType type) {
        return switch (type) {
            case ELECT -> "ELECT";
            case ELECT_MONARCH -> "ELECT MONARCH";
            case DEMOTE -> "DEMOTE";
        };
    }

    /**
     * Formats a remaining duration as "Xh Ym", or just "Ym" once under an
     * hour remains.
     *
     * @param remainingMillis the remaining duration, in milliseconds
     * @return the formatted string, or "expired" if zero or negative
     */
    private String formatTimeRemaining(long remainingMillis) {
        if (remainingMillis <= 0) return "expired";
        long totalMinutes = remainingMillis / 60_000;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return (hours > 0) ? (hours + "h " + minutes + "m") : (minutes + "m");
    }
}