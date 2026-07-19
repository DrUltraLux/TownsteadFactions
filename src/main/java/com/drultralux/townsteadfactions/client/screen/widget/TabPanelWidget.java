package com.drultralux.townsteadfactions.client.screen.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * A single dashboard tab: its clickable header button and the widgets
 * assigned to it. Tabs themselves are not draggable; only the widgets
 * they contain are.
 *
 * <p>Instances should only be created, renamed, reordered, or have
 * widgets assigned through {@code TabManager} — other classes are only
 * expected to render and hit-test the tabs they're handed, not mutate
 * this class's identity or membership directly.</p>
 */
public class TabPanelWidget {

    /** Maximum time between two clicks, in milliseconds, to register as a double click. */
    private static final long DOUBLE_CLICK_WINDOW_MS = 400;

    /** The fixed height of a tab's header button. */
    private static final int HEADER_HEIGHT = 14;

    /** The width reserved at the end of the header for the close ("×") button. */
    private static final int CLOSE_ZONE_WIDTH = 12;

    /** The result of handling a click on this tab's header. */
    public enum ClickResult {
        /** The click missed this tab's header entirely. */
        NONE,
        /** A single click selected this tab. */
        SELECTED,
        /** A double click requested this tab be renamed. */
        RENAME_REQUESTED,
        /** A click on the close button requested this tab be removed. */
        CLOSE_REQUESTED
    }

    /** This tab's stable identifier, unaffected by reordering or renaming. */
    private final String id;

    /** This tab's current display name. */
    private String title;

    /** The widgets currently assigned to this tab. */
    private final List<DraggableWidget> components = new ArrayList<>();

    /** This tab header's current x position, assigned once per frame by the tab layout pass. */
    private int headerX;

    /** This tab header's current y position, assigned once per frame by the tab layout pass. */
    private int headerY;

    /** This tab header's current text width, assigned once per frame by the tab layout pass. */
    private int headerWidth;

    /** The system time, in milliseconds, of the last click on this tab's header, or {@code -1} if none yet. */
    private long lastClickTimeMs = -1;

    /**
     * How far this tab's content is currently scrolled down, in pixels.
     * Each tab remembers its own scroll position independently, so
     * switching tabs and switching back preserves where you were.
     */
    private int scrollOffsetY = 0;

    /**
     * Creates a new tab with no widgets assigned.
     *
     * @param id this tab's stable identifier
     * @param title this tab's initial display name
     */
    public TabPanelWidget(String id, String title) {
        this.id = id;
        this.title = title;
    }

    /**
     * Returns this tab's stable identifier.
     *
     * @return the tab ID
     */
    public String getId() {
        return this.id;
    }

    /**
     * Returns this tab's current display name.
     *
     * @return the tab title
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * Sets this tab's display name.
     *
     * @param title the new title; ignored if {@code null} or blank
     */
    public void setTitle(String title) {
        if (title != null && !title.trim().isEmpty()) {
            this.title = title.trim();
        }
    }

    /**
     * Adds a widget to this tab, if not already present.
     *
     * @param widget the widget to add
     */
    public void addWidget(DraggableWidget widget) {
        if (widget != null && !this.components.contains(widget)) {
            this.components.add(widget);
        }
    }

    /**
     * Removes a widget from this tab.
     *
     * @param widget the widget to remove
     */
    public void removeWidget(DraggableWidget widget) {
        this.components.remove(widget);
    }

    /**
     * Returns the live list of widgets assigned to this tab. Modifying the
     * returned list modifies this tab directly.
     *
     * @return the widgets assigned to this tab
     */
    public List<DraggableWidget> getComponents() {
        return this.components;
    }

    /**
     * Assigns this tab header's on-screen bounds. Called once per frame,
     * before any rendering or hit-testing, by the shared tab layout pass.
     *
     * @param x the header's baseline x position
     * @param y the header's baseline y position
     * @param width the header's text width, excluding padding and the close zone
     */
    public void setHeaderBounds(int x, int y, int width) {
        this.headerX = x;
        this.headerY = y;
        this.headerWidth = width;
    }

    /**
     * Returns this tab header's current baseline x position.
     *
     * @return the header's x position
     */
    public int getHeaderX() {
        return this.headerX;
    }

    /**
     * Returns this tab header's current baseline y position.
     *
     * @return the header's y position
     */
    public int getHeaderY() {
        return this.headerY;
    }

    /**
     * Returns this tab header's current text width, excluding padding and
     * the close zone.
     *
     * @return the header's text width
     */
    public int getHeaderWidth() {
        return this.headerWidth;
    }

    /**
     * Returns this tab header's total on-screen width, including its text,
     * padding, and the close button zone. Used by {@code TabManager} to
     * space consecutive tab headers correctly.
     *
     * @return the header's total width
     */
    public int getTotalHeaderWidth() {
        return this.headerWidth + CLOSE_ZONE_WIDTH;
    }

    /**
     * Checks whether the given point falls within this tab's clickable
     * header bounds (including padding and the close zone).
     *
     * @param mouseX the x position to check
     * @param mouseY the y position to check
     * @return {@code true} if the point is within the header
     */
    public boolean isHeaderHovered(double mouseX, double mouseY) {
        return mouseX >= this.headerX - 4 && mouseX <= this.headerX + getTotalHeaderWidth() + 4 &&
                mouseY >= this.headerY - 2 && mouseY <= this.headerY + 12;
    }

    /**
     * Checks whether the given point falls within this tab's close button.
     *
     * @param mouseX the x position to check
     * @param mouseY the y position to check
     * @return {@code true} if the point is within the close button
     */
    public boolean isCloseHovered(double mouseX, double mouseY) {
        return mouseX >= this.headerX + this.headerWidth && mouseX <= this.headerX + getTotalHeaderWidth() &&
                mouseY >= this.headerY - 2 && mouseY <= this.headerY + 12;
    }

    /**
     * Handles a click at the given position. A click on the close button
     * (only checked if {@code closeable}) requests removal; otherwise, a
     * click within this tab's header distinguishes a single click (select
     * this tab) from a double click within
     * {@value #DOUBLE_CLICK_WINDOW_MS}ms of the previous one (request a
     * rename).
     *
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     * @param closeable whether this tab is currently allowed to be closed
     *                  (there must be at least one other tab remaining)
     * @return the result of the click
     */
    public ClickResult mouseClicked(double mouseX, double mouseY, boolean closeable) {
        if (closeable && isCloseHovered(mouseX, mouseY)) {
            return ClickResult.CLOSE_REQUESTED;
        }

        if (!isHeaderHovered(mouseX, mouseY)) {
            return ClickResult.NONE;
        }

        long now = System.currentTimeMillis();
        boolean isDoubleClick = (this.lastClickTimeMs >= 0) && (now - this.lastClickTimeMs <= DOUBLE_CLICK_WINDOW_MS);
        this.lastClickTimeMs = now;

        return isDoubleClick ? ClickResult.RENAME_REQUESTED : ClickResult.SELECTED;
    }

    /**
     * Renders this tab's header button, including its close button if
     * {@code closeable}.
     *
     * @param graphics the graphics context to draw with
     * @param font the font to draw the title with
     * @param isActive whether this tab is currently the selected tab
     * @param closeable whether to render the close button
     */
    public void renderHeader(GuiGraphics graphics, Font font, boolean isActive, boolean closeable) {
        int titleColor = isActive ? 0xFFFFFFFF : 0xFFAAAAAA;
        int totalWidth = getTotalHeaderWidth();

        graphics.fill(this.headerX - 4, this.headerY - 2, this.headerX + totalWidth + 4, this.headerY + 12, isActive ? 0xFF333333 : 0xFF222222);
        graphics.renderOutline(this.headerX - 4, this.headerY - 2, totalWidth + 8, HEADER_HEIGHT, 0xFF555555);
        graphics.drawString(font, Component.literal(this.title), this.headerX, this.headerY + 1, titleColor, false);

        if (closeable) {
            graphics.drawString(font, "×", this.headerX + this.headerWidth + 2, this.headerY + 1, 0xFFCC5555, false);
        }
    }

    /**
     * Renders every widget assigned to this tab.
     *
     * @param graphics the graphics context to draw with
     * @param mouseX the current mouse x position
     * @param mouseY the current mouse y position
     * @param partialTicks the partial tick time, for frame interpolation
     */
    public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        for (DraggableWidget widget : this.components) {
            widget.render(graphics, mouseX, mouseY, partialTicks);
        }
    }

    /**
     * Returns how far this tab's content is currently scrolled down.
     *
     * @return the current scroll offset, in pixels
     */
    public int getScrollOffsetY() {
        return this.scrollOffsetY;
    }

    /**
     * Sets how far this tab's content is scrolled down. Callers are
     * responsible for clamping this to a valid range — this setter
     * applies whatever value it's given as-is.
     *
     * @param scrollOffsetY the new scroll offset, in pixels
     */
    public void setScrollOffsetY(int scrollOffsetY) {
        this.scrollOffsetY = scrollOffsetY;
    }
}