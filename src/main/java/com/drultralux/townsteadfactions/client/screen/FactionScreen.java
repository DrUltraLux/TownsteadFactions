package com.drultralux.townsteadfactions.client.screen;

import com.drultralux.townsteadfactions.client.KeyMappings;
import com.drultralux.townsteadfactions.client.screen.widget.ActivityLogWidget;
import com.drultralux.townsteadfactions.client.screen.widget.DraggableWidget;
import com.drultralux.townsteadfactions.client.screen.widget.GlobalFactionsWidget;
import com.drultralux.townsteadfactions.client.screen.widget.PlayerModelWidget;
import com.drultralux.townsteadfactions.client.screen.widget.ResourceDisplayWidget;
import com.drultralux.townsteadfactions.client.ClientFactionCache;
import com.drultralux.townsteadfactions.client.screen.widget.LeadershipManagementWidget;
import com.drultralux.townsteadfactions.client.screen.widget.RosterDisplayWidget;
import com.drultralux.townsteadfactions.client.screen.widget.TabPanelWidget;
import com.drultralux.townsteadfactions.client.screen.widget.VotingWidget;
import com.drultralux.townsteadfactions.client.screen.widget.VillageMapWidget;
import com.drultralux.townsteadfactions.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * The main faction dashboard screen: a resizable, draggable, multi-tab,
 * vertically-scrollable window hosting the treasury, roster, global
 * factions, avatar, and activity log widgets. Tab structure and widget
 * placement are owned entirely by {@link TabManager}; this screen only
 * renders what it's given and forwards input events into
 * {@code TabManager}'s methods.
 */
public class FactionScreen extends Screen {

    /**
     * Identifies which corner, if any, is currently being dragged to
     * resize the window. Each corner keeps the opposite corner fixed in
     * place while resizing — {@code BOTTOM_RIGHT} keeps the top-left
     * corner fixed and grows right/down; {@code BOTTOM_LEFT} keeps the
     * top-right corner fixed and grows left/down. Neither corner ever
     * moves the window's top edge.
     */
    private enum ResizeCorner { NONE, BOTTOM_LEFT, BOTTOM_RIGHT }

    /**
     * The minimum window width for the resize currently in progress,
     * captured once when the resize begins (from
     * {@link TabManager#getGlobalMinimumWidth}) and held fixed for the
     * duration of that drag, rather than being recomputed every frame.
     */
    private int resizeMinWidth = 200;

    /** Fixed minimum window height, in pixels. */
    private static final int MIN_BOX_HEIGHT = 160;

    /** The size, in pixels, of each corner's resize-handle click zone. */
    private static final int RESIZE_HANDLE_SIZE = 10;

    /** The width, in pixels, of the vertical scrollbar track. */
    private static final int SCROLLBAR_WIDTH = 6;

    /** The minimum height, in pixels, the scrollbar thumb is ever drawn at, so it stays grabbable even with a lot of content. */
    private static final int SCROLLBAR_MIN_THUMB_HEIGHT = 12;

    /** The widget currently being dragged by the mouse, or {@code null} if none. */
    private DraggableWidget activeDraggedComponent = null;

    /** The current width of the main dashboard window, in pixels. */
    private int boxWidth;

    /** The current height of the main dashboard window, in pixels. */
    private int boxHeight;

    /**
     * The window's horizontal offset, in pixels, from its default
     * centered position on screen. Zero means centered; positive moves
     * right, negative moves left. Updated by dragging the window and
     * persisted to config.
     */
    private int windowOffsetX = 0;

    /**
     * The window's vertical offset, in pixels, from its default centered
     * position on screen. Zero means centered; positive moves down,
     * negative moves up. Updated by dragging the window and persisted to
     * config.
     */
    private int windowOffsetY = 0;

    /** Which corner, if any, is currently being dragged to resize the window. */
    private ResizeCorner activeResizeCorner = ResizeCorner.NONE;

    /** The mouse x offset captured when a bottom-right resize begins (mouseX minus boxWidth at that moment). */
    private int resizeOffsetX;

    /** The mouse y offset captured when a resize begins (mouseY minus boxHeight at that moment); used by both resize corners, since height always grows downward from the fixed top edge regardless of which corner is dragged. */
    private int resizeOffsetY;

    /**
     * The window's fixed right edge (in absolute screen pixels), captured
     * when a bottom-left resize begins. A bottom-left resize keeps this
     * edge constant and grows the window leftward from it.
     */
    private int resizeFixedRightEdge;

    /**
     * The window's fixed left edge (in absolute screen pixels), captured
     * when a bottom-right resize begins. Needed because
     * {@link #getMainX()}'s centered-position formula has {@code boxWidth}
     * baked into it — so without this, the left edge would silently drift
     * every time {@code boxWidth} changes, even though nothing here ever
     * touches {@code windowOffsetX} directly during that resize.
     */
    private int resizeFixedLeftEdge;

    /**
     * The window's fixed top edge (in absolute screen pixels), captured
     * when any resize begins. Both resize corners keep the top edge
     * fixed and only grow downward — but {@link #getMainY()}'s centered
     * formula has {@code boxHeight} baked into it the same way
     * {@link #resizeFixedLeftEdge} addresses for width, so this must be
     * explicitly re-applied every drag tick too, for both corners.
     */
    private int resizeFixedTopEdge;

    /** Whether the window itself (its empty background, not a widget or control) is currently being dragged to move it. */
    private boolean isDraggingWindow = false;

    /** The mouse x position captured when a window drag begins. */
    private double windowDragStartMouseX;

    /** The mouse y position captured when a window drag begins. */
    private double windowDragStartMouseY;

    /** The window's {@link #windowOffsetX} captured when a window drag begins. */
    private int windowDragStartOffsetX;

    /** The window's {@link #windowOffsetY} captured when a window drag begins. */
    private int windowDragStartOffsetY;

    /** The {@link #windowOffsetX} value last applied to every widget's position during the current window drag. */
    private int windowDragLastAppliedOffsetX;

    /** The {@link #windowOffsetY} value last applied to every widget's position during the current window drag. */
    private int windowDragLastAppliedOffsetY;

    /** Whether the vertical scrollbar's thumb is currently being dragged. */
    private boolean isDraggingScrollbar = false;

    /** The mouse y position captured when a scrollbar drag begins. */
    private double scrollbarDragStartMouseY;

    /** The active tab's scroll offset captured when a scrollbar drag begins. */
    private int scrollbarDragStartOffset;

    /** The treasury/resources widget instance. */
    private ResourceDisplayWidget treasuryWidget;

    /** The faction roster widget instance. */
    private RosterDisplayWidget rosterWidget;

    /** The global factions overview widget instance. */
    private GlobalFactionsWidget globalWidget;

    /** The player avatar preview widget instance. */
    private PlayerModelWidget avatarWidget;

    /** The activity log widget instance. */
    private ActivityLogWidget activityWidget;

    /** The voting widget instance. */
    private VotingWidget votingWidget;

    /** The leadership management widget instance, only ever placed on the conditional Leadership tab. */
    private LeadershipManagementWidget leadershipWidget;

    private VillageMapWidget villageMapWidget;

    /** The active tab-rename text field, or {@code null} if no rename is in progress. */
    private EditBox tabRenameBox = null;

    /** The ID of the tab currently being renamed, or {@code null} if none. */
    private String renamingTabId = null;

    /**
     * Creates the faction dashboard screen.
     */
    public FactionScreen() {
        super(Component.literal("Faction Dashboard"));
    }

    /**
     * Loads the saved tab layout (or installs defaults, on first run or
     * after a layout version bump), then creates and places this screen's
     * widgets according to their saved tab assignments.
     */
    @Override
    protected void init() {
        super.init();

        this.boxWidth = ModConfig.CLIENT.getInteger("mainBoxWidth", 360);
        this.boxHeight = ModConfig.CLIENT.getInteger("mainBoxHeight", 220);
        this.windowOffsetX = ModConfig.CLIENT.getInteger("mainBoxOffsetX", 0);
        this.windowOffsetY = ModConfig.CLIENT.getInteger("mainBoxOffsetY", 0);

        int savedVersion = ModConfig.CLIENT.getInteger("savedLayoutVersion", 0);
        boolean needsVersionReset = savedVersion < TabManager.CURRENT_LAYOUT_VERSION;

        boolean loaded = false;
        if (!needsVersionReset) {
            List<String> tabMappings = ModConfig.CLIENT.getStringList("customizedTabOrder");
            loaded = TabManager.loadFromEncoded(tabMappings);
        }

        if (!loaded) {
            ScreenLayoutSaver.resetToDefaults();
            if (needsVersionReset) {
                ScreenLayoutSaver.saveLayoutVersion(TabManager.CURRENT_LAYOUT_VERSION);
            }
        }

        int mainX = getMainX();
        int mainY = getMainY();

        this.treasuryWidget = new ResourceDisplayWidget(mainX + 20 + ModConfig.CLIENT.getInteger("treasuryWidgetX", -50), mainY + 40 + ModConfig.CLIENT.getInteger("treasuryWidgetY", -30));
        this.rosterWidget = new RosterDisplayWidget(mainX + 20 + ModConfig.CLIENT.getInteger("rosterWidgetX", 40), mainY + 40 + ModConfig.CLIENT.getInteger("rosterWidgetY", -10));
        this.globalWidget = new GlobalFactionsWidget(mainX + 20 + ModConfig.CLIENT.getInteger("globalWidgetX", -100), mainY + 40 + ModConfig.CLIENT.getInteger("globalWidgetY", 10));
        this.avatarWidget = new PlayerModelWidget(mainX + 20 + ModConfig.CLIENT.getInteger("avatarWidgetX", 20), mainY + 40 + ModConfig.CLIENT.getInteger("avatarWidgetY", -10));
        this.activityWidget = new ActivityLogWidget(mainX + 20 + ModConfig.CLIENT.getInteger("activityWidgetX", -40), mainY + 40 + ModConfig.CLIENT.getInteger("activityWidgetY", 90));
        this.votingWidget = new VotingWidget(mainX + 20 + ModConfig.CLIENT.getInteger("votingWidgetX", 250), mainY + 40 + ModConfig.CLIENT.getInteger("votingWidgetY", -30));
        this.leadershipWidget = new LeadershipManagementWidget(mainX + 40, mainY + 40);
        this.villageMapWidget = new VillageMapWidget(mainX + 20 + ModConfig.CLIENT.getInteger("villageMapWidgetX", -100), mainY + 40 + ModConfig.CLIENT.getInteger("villageMapWidgetY", 40));

        placeWidget(this.treasuryWidget, "treasuryWidgetTabId", TabManager.DEFAULT_TAB_OVERVIEW);
        placeWidget(this.rosterWidget, "rosterWidgetTabId", TabManager.DEFAULT_TAB_ROSTER);
        placeWidget(this.globalWidget, "globalWidgetTabId", TabManager.DEFAULT_TAB_GLOBAL);
        placeWidget(this.avatarWidget, "avatarWidgetTabId", TabManager.DEFAULT_TAB_OVERVIEW);
        placeWidget(this.activityWidget, "activityWidgetTabId", TabManager.DEFAULT_TAB_OVERVIEW);
        placeWidget(this.votingWidget, "votingWidgetTabId", TabManager.DEFAULT_TAB_OVERVIEW);
        placeWidget(this.villageMapWidget, "villageMapWidgetTabId", TabManager.DEFAULT_TAB_GLOBAL);

        this.treasuryWidget.setMinimized(ModConfig.CLIENT.getBoolean("treasuryWidgetMinimized", false));
        this.rosterWidget.setMinimized(ModConfig.CLIENT.getBoolean("rosterWidgetMinimized", false));
        this.globalWidget.setMinimized(ModConfig.CLIENT.getBoolean("globalWidgetMinimized", false));
        this.avatarWidget.setMinimized(ModConfig.CLIENT.getBoolean("avatarWidgetMinimized", false));
        this.activityWidget.setMinimized(ModConfig.CLIENT.getBoolean("activityWidgetMinimized", false));
        this.votingWidget.setMinimized(ModConfig.CLIENT.getBoolean("votingWidgetMinimized", false));
        this.villageMapWidget.setMinimized(ModConfig.CLIENT.getBoolean("villageMapWidgetMinimized", false));

        // The Leadership tab may already exist at this point if it was open and got persisted
        // into customizedTabOrder on a previous session's close — in that case it's restored by
        // loadFromEncoded() above, before this method ever ran, so the render()-time sync logic
        // (which only fires when the tab is freshly created this session) never gets a chance to
        // attach the widget to it. Handle that "already existed on load" case explicitly here.
        if (TabManager.findTab(TabManager.DEFAULT_TAB_LEADERSHIP) != null) {
            TabManager.moveWidgetToTab(this.leadershipWidget, TabManager.DEFAULT_TAB_LEADERSHIP);
        }
    }

    /**
     * Places a widget onto its saved tab, reading the saved tab ID from
     * config under the given key.
     *
     * @param widget the widget to place
     * @param configKey the config key holding the widget's saved tab ID
     * @param fallbackTabId the tab ID to use if no saved value exists
     */
    private void placeWidget(DraggableWidget widget, String configKey, String fallbackTabId) {
        String tabId = ModConfig.CLIENT.getString(configKey, fallbackTabId);
        TabManager.moveWidgetToTab(widget, tabId);
    }

    /**
     * Computes the window's current absolute x position on screen: its
     * default centered position, plus any drag offset.
     *
     * @return the window's left edge x position, in absolute screen pixels
     */
    private int getMainX() {
        return this.width / 2 - (this.boxWidth / 2) + this.windowOffsetX;
    }

    /**
     * Computes the window's current absolute y position on screen: its
     * default centered position, plus any drag offset.
     *
     * @return the window's top edge y position, in absolute screen pixels
     */
    private int getMainY() {
        return this.height / 2 - (this.boxHeight / 2) + this.windowOffsetY;
    }

    /**
     * Computes the top edge of the scrollable content area (below the tab
     * headers), for the window's current position.
     *
     * @param mainY the window's current top edge y position
     * @return the content area's top edge y position
     */
    private int getContentTop(int mainY) {
        return mainY + 24;
    }

    /**
     * Computes the bottom edge of the scrollable content area, for the
     * window's current position and size.
     *
     * @param mainY the window's current top edge y position
     * @return the content area's bottom edge y position
     */
    private int getContentBottom(int mainY) {
        return mainY + this.boxHeight - 4;
    }

    /**
     * Computes the right edge of the scrollable content area, reserving
     * space for the vertical scrollbar so widgets never sit underneath
     * it.
     *
     * @param mainX the window's current left edge x position
     * @return the content area's right edge x position
     */
    private int getContentRight(int mainX) {
        return mainX + this.boxWidth - 4 - SCROLLBAR_WIDTH;
    }

    /**
     * Computes how far a tab's content currently extends below the
     * content area's top edge, based on the deepest widget it contains.
     *
     * @param tab the tab to measure
     * @param contentTop the content area's top edge y position
     * @return the required content height, in pixels; never less than zero
     */
    private int computeRequiredContentHeight(TabPanelWidget tab, int contentTop) {
        int deepestBottom = contentTop;
        for (DraggableWidget widget : tab.getComponents()) {
            deepestBottom = Math.max(deepestBottom, widget.getY() + widget.getHeight());
        }
        return Math.max(0, deepestBottom - contentTop);
    }

    /**
     * Computes the maximum valid scroll offset for a tab: how far past
     * the visible content area its content currently extends.
     *
     * @param tab the tab to measure
     * @param contentTop the content area's top edge y position
     * @param contentBottom the content area's bottom edge y position
     * @return the maximum scroll offset, in pixels; zero if content fits without scrolling
     */
    private int computeMaxScroll(TabPanelWidget tab, int contentTop, int contentBottom) {
        int requiredHeight = computeRequiredContentHeight(tab, contentTop);
        int visibleHeight = contentBottom - contentTop;
        return Math.max(0, requiredHeight - visibleHeight);
    }

    /**
     * Renders the dashboard background, window frame, both resize
     * handles, tab headers, add-tab button, the active tab's
     * scroll-clipped widgets, and the vertical scrollbar if needed.
     *
     * @param graphics the graphics context to draw with
     * @param mouseX the current mouse x position
     * @param mouseY the current mouse y position
     * @param partialTicks the partial tick time, for frame interpolation
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        boolean justCreatedLeadershipTab = TabManager.syncLeadershipTabVisibility(ClientFactionCache.isLocalPlayerLeader());
        if (justCreatedLeadershipTab) {
            TabManager.moveWidgetToTab(this.leadershipWidget, TabManager.DEFAULT_TAB_LEADERSHIP);
        }

        int overlayTopColor = 0xC0101010;
        int overlayBottomColor = 0xD00A0A0A;
        graphics.fillGradient(0, 0, this.width, this.height, overlayTopColor, overlayBottomColor);

        int mainX = getMainX();
        int mainY = getMainY();

        graphics.fill(mainX, mainY, mainX + this.boxWidth, mainY + this.boxHeight, 0xDD111111);
        graphics.renderOutline(mainX, mainY, this.boxWidth, this.boxHeight, 0xFF444444);

        // Bottom-right resize handle
        int handleRightX = mainX + this.boxWidth - RESIZE_HANDLE_SIZE;
        int handleRightY = mainY + this.boxHeight - RESIZE_HANDLE_SIZE;
        graphics.fill(handleRightX, handleRightY, handleRightX + RESIZE_HANDLE_SIZE, handleRightY + RESIZE_HANDLE_SIZE, 0xFF666666);

        // Bottom-left resize handle
        int handleLeftX = mainX;
        int handleLeftY = mainY + this.boxHeight - RESIZE_HANDLE_SIZE;
        graphics.fill(handleLeftX, handleLeftY, handleLeftX + RESIZE_HANDLE_SIZE, handleLeftY + RESIZE_HANDLE_SIZE, 0xFF666666);

        TabManager.layoutHeaders(mainX, mainY, this.font);
        String activeTabId = TabManager.getActiveTabId();
        for (TabPanelWidget tab : TabManager.getTabs()) {
            if (!tab.getId().equals(this.renamingTabId)) {
                boolean closeable = !TabManager.isProtectedTab(tab.getId()) && TabManager.getRegularTabCount() > 1;
                tab.renderHeader(graphics, this.font, tab.getId().equals(activeTabId), closeable);
            }
        }
        TabManager.renderAddButton(graphics, this.font);

        TabPanelWidget activeTab = TabManager.getActiveTab();
        if (activeTab != null) {
            int contentTop = getContentTop(mainY);
            int contentBottom = getContentBottom(mainY);
            int contentLeft = mainX + 4;
            int contentRight = getContentRight(mainX);

            // Horizontal clamp (unchanged from before) and a top-only vertical clamp —
            // widgets are allowed to extend below the visible area now; that's what scrolling reveals.
            for (DraggableWidget widget : activeTab.getComponents()) {
                if (widget.getX() < contentLeft) widget.setPosition(contentLeft, widget.getY());
                if (widget.getX() + widget.getWidth() > contentRight) widget.setPosition(contentRight - widget.getWidth(), widget.getY());
                if (widget.getY() < contentTop) widget.setPosition(widget.getX(), contentTop);
            }

            int maxScroll = computeMaxScroll(activeTab, contentTop, contentBottom);
            int scrollOffset = Math.max(0, Math.min(activeTab.getScrollOffsetY(), maxScroll));
            activeTab.setScrollOffsetY(scrollOffset);

            graphics.enableScissor(contentLeft, contentTop, contentRight, contentBottom);
            graphics.pose().pushPose();
            graphics.pose().translate(0, -scrollOffset, 0);
            activeTab.renderContent(graphics, mouseX, mouseY + scrollOffset, partialTicks);
            graphics.pose().popPose();
            graphics.disableScissor();

            if (maxScroll > 0) {
                renderScrollbar(graphics, mainX, contentTop, contentBottom, scrollOffset, maxScroll);
            }
        }

        // Render our own widgets (including an active rename EditBox, if any) directly,
        // bypassing the parent Screen's background/blur pass.
        for (Renderable renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTicks);
        }
    }

    /**
     * Renders the vertical scrollbar track and thumb along the content
     * area's right edge.
     *
     * @param graphics the graphics context to draw with
     * @param mainX the window's current left edge x position
     * @param contentTop the content area's top edge y position
     * @param contentBottom the content area's bottom edge y position
     * @param scrollOffset the active tab's current scroll offset
     * @param maxScroll the active tab's current maximum scroll offset
     */
    private void renderScrollbar(GuiGraphics graphics, int mainX, int contentTop, int contentBottom, int scrollOffset, int maxScroll) {
        int trackX = getContentRight(mainX) + 2;
        int trackHeight = contentBottom - contentTop;

        graphics.fill(trackX, contentTop, trackX + SCROLLBAR_WIDTH, contentBottom, 0xFF1A1A1A);

        int[] thumbBounds = computeScrollbarThumbBounds(contentTop, trackHeight, scrollOffset, maxScroll);
        graphics.fill(trackX, thumbBounds[0], trackX + SCROLLBAR_WIDTH, thumbBounds[1], 0xFF888888);
    }

    /**
     * Computes the scrollbar thumb's top and bottom y positions, sized
     * proportionally to how much of the content is currently visible, and
     * positioned proportionally to the current scroll offset.
     *
     * @param contentTop the content area's top edge y position
     * @param trackHeight the scrollbar track's total height
     * @param scrollOffset the active tab's current scroll offset
     * @param maxScroll the active tab's current maximum scroll offset
     * @return a two-element array: {@code [thumbTop, thumbBottom]}
     */
    private int[] computeScrollbarThumbBounds(int contentTop, int trackHeight, int scrollOffset, int maxScroll) {
        int visibleHeight = trackHeight;
        int totalHeight = trackHeight + maxScroll;
        int thumbHeight = Math.max(SCROLLBAR_MIN_THUMB_HEIGHT, (int) ((long) visibleHeight * trackHeight / totalHeight));
        int maxThumbTravel = trackHeight - thumbHeight;
        int thumbTop = contentTop + (maxScroll > 0 ? (int) ((long) maxThumbTravel * scrollOffset / maxScroll) : 0);
        return new int[] { thumbTop, thumbTop + thumbHeight };
    }

    /**
     * Checks whether a point falls within the active tab's scrollbar
     * thumb, if one is currently visible.
     *
     * @param mouseX the x position to check
     * @param mouseY the y position to check
     * @return {@code true} if the point is within the visible scrollbar thumb
     */
    private boolean isScrollbarThumbHovered(double mouseX, double mouseY) {
        TabPanelWidget activeTab = TabManager.getActiveTab();
        if (activeTab == null) return false;

        int mainX = getMainX();
        int mainY = getMainY();
        int contentTop = getContentTop(mainY);
        int contentBottom = getContentBottom(mainY);
        int maxScroll = computeMaxScroll(activeTab, contentTop, contentBottom);
        if (maxScroll <= 0) return false;

        int trackX = getContentRight(mainX) + 2;
        if (mouseX < trackX || mouseX > trackX + SCROLLBAR_WIDTH) return false;

        int[] thumbBounds = computeScrollbarThumbBounds(contentTop, contentBottom - contentTop, activeTab.getScrollOffsetY(), maxScroll);
        return mouseY >= thumbBounds[0] && mouseY <= thumbBounds[1];
    }

    /**
     * Intentionally left blank so vanilla's background blur shader pass is
     * skipped for this screen.
     *
     * @param graphics the graphics context, unused
     * @param mouseX the current mouse x position, unused
     * @param mouseY the current mouse y position, unused
     * @param partialTicks the partial tick time, unused
     */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Intentionally empty. TODO: consider a 3D render layer here.
    }

    /**
     * Handles mouse clicks, in priority order: committing an in-progress
     * tab rename if clicking elsewhere, then (in order) the two resize
     * handles, the add-tab button, tab header interaction, the scrollbar
     * thumb, beginning a widget drag (with the click's y position adjusted
     * for the active tab's current scroll), and finally — if nothing else
     * claimed the click and it landed on the window's empty background —
     * beginning a window drag.
     *
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     * @param button the mouse button pressed
     * @return {@code true} if the click was handled
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.tabRenameBox != null && !this.tabRenameBox.isMouseOver(mouseX, mouseY)) {
            commitRename();
        }

        if (button == 0) {
            int mainX = getMainX();
            int mainY = getMainY();

            // Bottom-right resize handle: keeps the top-left corner fixed, grows right/down.
            if (mouseX >= mainX + this.boxWidth - RESIZE_HANDLE_SIZE && mouseX <= mainX + this.boxWidth
                    && mouseY >= mainY + this.boxHeight - RESIZE_HANDLE_SIZE && mouseY <= mainY + this.boxHeight) {
                this.activeResizeCorner = ResizeCorner.BOTTOM_RIGHT;
                this.resizeOffsetX = (int) mouseX - this.boxWidth;
                this.resizeOffsetY = (int) mouseY - this.boxHeight;
                this.resizeFixedLeftEdge = mainX;
                this.resizeFixedTopEdge = mainY;
                this.resizeMinWidth = TabManager.getGlobalMinimumWidth(mainX);
                return true;
            }

            // Bottom-left resize handle: keeps the top-right corner fixed, grows left/down.
            if (mouseX >= mainX && mouseX <= mainX + RESIZE_HANDLE_SIZE
                    && mouseY >= mainY + this.boxHeight - RESIZE_HANDLE_SIZE && mouseY <= mainY + this.boxHeight) {
                this.activeResizeCorner = ResizeCorner.BOTTOM_LEFT;
                this.resizeFixedRightEdge = mainX + this.boxWidth;
                this.resizeFixedTopEdge = mainY;
                this.resizeOffsetY = (int) mouseY - this.boxHeight;
                this.resizeMinWidth = TabManager.getGlobalMinimumWidth(mainX);
                return true;
            }

            if (TabManager.isAddButtonHovered(mouseX, mouseY)) {
                String newTabId = TabManager.addTab("New Tab");
                TabManager.setActiveTab(newTabId);
                beginRenameTab(newTabId);
                return true;
            }

            TabManager.HeaderClickOutcome headerClick = TabManager.handleHeaderClick(mouseX, mouseY);
            if (headerClick.consumed()) {
                if (headerClick.renameRequestedTabId() != null) {
                    beginRenameTab(headerClick.renameRequestedTabId());
                } else if (headerClick.closeRequestedTabId() != null) {
                    if (headerClick.closeRequestedTabId().equals(this.renamingTabId)) {
                        cancelRename();
                    }
                    TabManager.removeTab(headerClick.closeRequestedTabId());
                    flushPlacementsToConfig();
                }
                return true;
            }

            if (isScrollbarThumbHovered(mouseX, mouseY)) {
                TabPanelWidget activeTabForScroll = TabManager.getActiveTab();
                if (activeTabForScroll != null) {
                    this.isDraggingScrollbar = true;
                    this.scrollbarDragStartMouseY = mouseY;
                    this.scrollbarDragStartOffset = activeTabForScroll.getScrollOffsetY();
                    return true;
                }
            }

            TabPanelWidget activeTab = TabManager.getActiveTab();
            if (activeTab != null) {
                double contentMouseY = mouseY + activeTab.getScrollOffsetY();
                for (DraggableWidget widget : activeTab.getComponents()) {
                    if (widget.mouseClicked(mouseX, contentMouseY, button)) {
                        this.activeDraggedComponent = widget;
                        return true;
                    }
                }
            }

            // Nothing else claimed the click. If it landed on the window's empty
            // background and window dragging is enabled, start dragging the window.
            boolean withinWindow = mouseX >= mainX && mouseX <= mainX + this.boxWidth
                    && mouseY >= mainY && mouseY <= mainY + this.boxHeight;
            if (withinWindow && ModConfig.CLIENT.getBoolean("allowWindowDragging", true)) {
                this.isDraggingWindow = true;
                this.windowDragStartMouseX = mouseX;
                this.windowDragStartMouseY = mouseY;
                this.windowDragStartOffsetX = this.windowOffsetX;
                this.windowDragStartOffsetY = this.windowOffsetY;
                this.windowDragLastAppliedOffsetX = this.windowOffsetX;
                this.windowDragLastAppliedOffsetY = this.windowOffsetY;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Forwards a mouse-wheel scroll to whichever widget in the active tab
     * is under the cursor (adjusted for the tab's current scroll, so a
     * widget's own internal scroll handling, like the activity log's,
     * sees the correct position); if no widget consumes it, scrolls the
     * active tab itself instead.
     *
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     * @param scrollX the horizontal scroll amount
     * @param scrollY the vertical scroll amount
     * @return {@code true} if the scroll was handled
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        TabPanelWidget activeTab = TabManager.getActiveTab();
        if (activeTab == null) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        double contentMouseY = mouseY + activeTab.getScrollOffsetY();
        for (DraggableWidget widget : activeTab.getComponents()) {
            if (widget.mouseScrolled(mouseX, contentMouseY, scrollX, scrollY)) {
                return true;
            }
        }

        int mainY = getMainY();
        int contentTop = getContentTop(mainY);
        int contentBottom = getContentBottom(mainY);
        int maxScroll = computeMaxScroll(activeTab, contentTop, contentBottom);
        if (maxScroll <= 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        int newOffset = activeTab.getScrollOffsetY() - (int) (scrollY * ModConfig.CLIENT.getInteger("dashboardScrollSpeed", 12));
        activeTab.setScrollOffsetY(Math.max(0, Math.min(maxScroll, newOffset)));
        return true;
    }

    /**
     * Handles dragging: resizing the window from whichever corner is
     * active (each keeping its opposite corner fixed by explicitly
     * re-pinning both window offsets every tick), moving the window
     * itself and every widget in every tab along with it if a window drag
     * is in progress, dragging the scrollbar thumb, or moving the
     * currently dragged widget (with its position adjusted for the active
     * tab's current scroll, and switching its tab if dropped over a
     * different tab header).
     *
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     * @param button the mouse button held
     * @param dragX the horizontal drag delta
     * @param dragY the vertical drag delta
     * @return {@code true} if the drag was handled
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.activeResizeCorner == ResizeCorner.BOTTOM_RIGHT) {
            this.boxWidth = Math.max(this.resizeMinWidth, (int) mouseX - this.resizeOffsetX);
            this.boxHeight = Math.max(MIN_BOX_HEIGHT, (int) mouseY - this.resizeOffsetY);

            // getMainX()/getMainY() both have boxWidth/boxHeight baked into their centered-position
            // formula, so both would silently drift as the window grows unless explicitly re-pinned
            // back to the fixed edges captured when this resize began.
            this.windowOffsetX = this.resizeFixedLeftEdge - (this.width / 2 - this.boxWidth / 2);
            this.windowOffsetY = this.resizeFixedTopEdge - (this.height / 2 - this.boxHeight / 2);
            return true;
        }

        if (this.activeResizeCorner == ResizeCorner.BOTTOM_LEFT) {
            int newBoxWidth = Math.max(this.resizeMinWidth, this.resizeFixedRightEdge - (int) mouseX);
            this.boxHeight = Math.max(MIN_BOX_HEIGHT, (int) mouseY - this.resizeOffsetY);

            // The right edge must stay fixed. Since getMainX() = width/2 - boxWidth/2 + windowOffsetX,
            // solve for the windowOffsetX that keeps (mainX + newBoxWidth) equal to the fixed right edge.
            int desiredMainX = this.resizeFixedRightEdge - newBoxWidth;
            this.boxWidth = newBoxWidth;
            this.windowOffsetX = desiredMainX - (this.width / 2 - this.boxWidth / 2);
            this.windowOffsetY = this.resizeFixedTopEdge - (this.height / 2 - this.boxHeight / 2);
            return true;
        }

        if (this.isDraggingWindow) {
            this.windowOffsetX = this.windowDragStartOffsetX + (int) (mouseX - this.windowDragStartMouseX);
            this.windowOffsetY = this.windowDragStartOffsetY + (int) (mouseY - this.windowDragStartMouseY);

            int deltaX = this.windowOffsetX - this.windowDragLastAppliedOffsetX;
            int deltaY = this.windowOffsetY - this.windowDragLastAppliedOffsetY;
            if (deltaX != 0 || deltaY != 0) {
                for (TabPanelWidget tab : TabManager.getTabs()) {
                    for (DraggableWidget widget : tab.getComponents()) {
                        widget.setPosition(widget.getX() + deltaX, widget.getY() + deltaY);
                    }
                }
                this.windowDragLastAppliedOffsetX = this.windowOffsetX;
                this.windowDragLastAppliedOffsetY = this.windowOffsetY;
            }
            return true;
        }

        if (this.isDraggingScrollbar) {
            TabPanelWidget activeTab = TabManager.getActiveTab();
            if (activeTab != null) {
                int mainY = getMainY();
                int contentTop = getContentTop(mainY);
                int contentBottom = getContentBottom(mainY);
                int maxScroll = computeMaxScroll(activeTab, contentTop, contentBottom);
                int trackHeight = contentBottom - contentTop;

                if (maxScroll > 0 && trackHeight > 0) {
                    double mouseDelta = mouseY - this.scrollbarDragStartMouseY;
                    int scrollDelta = (int) (mouseDelta * maxScroll / trackHeight);
                    int newOffset = this.scrollbarDragStartOffset + scrollDelta;
                    activeTab.setScrollOffsetY(Math.max(0, Math.min(maxScroll, newOffset)));
                }
            }
            return true;
        }

        if (this.activeDraggedComponent != null) {
            TabPanelWidget activeTab = TabManager.getActiveTab();
            double contentMouseY = (activeTab != null) ? mouseY + activeTab.getScrollOffsetY() : mouseY;
            this.activeDraggedComponent.mouseDragged(mouseX, contentMouseY, button, dragX, dragY);

            for (TabPanelWidget tab : TabManager.getTabs()) {
                if (tab.isHeaderHovered(mouseX, mouseY) && !tab.getId().equals(TabManager.getActiveTabId())) {
                    TabManager.moveWidgetToTab(this.activeDraggedComponent, tab.getId());
                    TabManager.setActiveTab(tab.getId());
                    break;
                }
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    /**
     * Ends any active resize, window drag, scrollbar drag, or widget
     * drag, and persists the resulting layout to the client config.
     *
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     * @param button the mouse button released
     * @return {@code true} if the release was handled
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.activeResizeCorner = ResizeCorner.NONE;
        this.isDraggingWindow = false;
        this.isDraggingScrollbar = false;
        if (this.activeDraggedComponent != null) {
            this.activeDraggedComponent.mouseReleased(mouseX, mouseY, button);
            this.activeDraggedComponent = null;
        }
        flushPlacementsToConfig();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * While renaming a tab, commits on Enter and cancels on Escape,
     * forwarding all other keys to the rename text field. Otherwise,
     * closes the dashboard when the dashboard hotkey or Escape is pressed.
     *
     * @param keyCode the key code pressed
     * @param scanCode the platform-specific scan code
     * @param modifiers the modifier key bitmask
     * @return {@code true} if the key press was handled
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.tabRenameBox != null) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                commitRename();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                cancelRename();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (KeyMappings.OPEN_FACTION_DASHBOARD.matches(keyCode, scanCode) || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Persists the current widget layout before closing the screen.
     */
    @Override
    public void onClose() {
        flushPlacementsToConfig();
        super.onClose();
    }

    /**
     * Opens a rename text field over the given tab's header, pre-filled
     * with its current title. Commits any rename already in progress
     * first.
     *
     * @param tabId the ID of the tab to rename
     */
    private void beginRenameTab(String tabId) {
        TabPanelWidget tab = TabManager.findTab(tabId);
        if (tab == null) return;

        if (this.tabRenameBox != null) {
            commitRename();
        }

        int boxWidth = Math.max(tab.getHeaderWidth() + 8, 80);
        EditBox box = new EditBox(this.font, tab.getHeaderX() - 4, tab.getHeaderY() - 2, boxWidth, 14, Component.literal("Tab name"));
        box.setMaxLength(24);
        box.setValue(tab.getTitle());
        box.setFocused(true);

        this.tabRenameBox = this.addRenderableWidget(box);
        this.renamingTabId = tabId;
        this.setFocused(this.tabRenameBox);
    }

    /**
     * Applies the rename box's current text to the tab being renamed,
     * closes the rename box, and persists the change.
     */
    private void commitRename() {
        if (this.tabRenameBox == null) return;
        TabManager.renameTab(this.renamingTabId, this.tabRenameBox.getValue());
        endRename();
        flushPlacementsToConfig();
    }

    /**
     * Closes the rename box without applying any change.
     */
    private void cancelRename() {
        endRename();
    }

    /**
     * Removes the active rename box and clears rename state, if any.
     */
    private void endRename() {
        if (this.tabRenameBox != null) {
            this.removeWidget(this.tabRenameBox);
            this.tabRenameBox = null;
            this.renamingTabId = null;
        }
    }

    /**
     * Converts each widget's absolute position to a position relative to
     * the window, resolves each widget's current tab, and saves the full
     * layout (widget positions/tabs, window size and drag offset, and tab
     * list) to the client config.
     */
    private void flushPlacementsToConfig() {
        int mainX = getMainX();
        int mainY = getMainY();

        ScreenLayoutSaver.saveFullLayout(
                this.treasuryWidget.getX() - mainX - 20, this.treasuryWidget.getY() - mainY - 40, findWidgetTabId(this.treasuryWidget),
                this.rosterWidget.getX() - mainX - 20, this.rosterWidget.getY() - mainY - 40, findWidgetTabId(this.rosterWidget),
                this.globalWidget.getX() - mainX - 20, this.globalWidget.getY() - mainY - 40, findWidgetTabId(this.globalWidget),
                this.avatarWidget.getX() - mainX - 20, this.avatarWidget.getY() - mainY - 40, findWidgetTabId(this.avatarWidget),
                this.activityWidget.getX() - mainX - 20, this.activityWidget.getY() - mainY - 40, findWidgetTabId(this.activityWidget),
                this.votingWidget.getX() - mainX - 20, this.votingWidget.getY() - mainY - 40, findWidgetTabId(this.votingWidget),
                this.villageMapWidget.getX() - mainX - 20, this.villageMapWidget.getY() - mainY - 40, findWidgetTabId(this.villageMapWidget),
                this.boxWidth, this.boxHeight,
                this.windowOffsetX, this.windowOffsetY,
                TabManager.getTabs()
        );

        ScreenLayoutSaver.saveMinimizedState("treasuryWidget", this.treasuryWidget.isMinimized());
        ScreenLayoutSaver.saveMinimizedState("rosterWidget", this.rosterWidget.isMinimized());
        ScreenLayoutSaver.saveMinimizedState("globalWidget", this.globalWidget.isMinimized());
        ScreenLayoutSaver.saveMinimizedState("avatarWidget", this.avatarWidget.isMinimized());
        ScreenLayoutSaver.saveMinimizedState("activityWidget", this.activityWidget.isMinimized());
        ScreenLayoutSaver.saveMinimizedState("votingWidget", this.votingWidget.isMinimized());
        ScreenLayoutSaver.saveMinimizedState("villageMapWidget", this.villageMapWidget.isMinimized());
    }

    /**
     * Finds the ID of the tab currently containing a widget.
     *
     * @param widget the widget to locate
     * @return the containing tab's ID, or the first tab's ID if not found
     */
    private String findWidgetTabId(DraggableWidget widget) {
        for (TabPanelWidget tab : TabManager.getTabs()) {
            if (tab.getComponents().contains(widget)) {
                return tab.getId();
            }
        }
        List<TabPanelWidget> tabs = TabManager.getTabs();
        return tabs.isEmpty() ? TabManager.DEFAULT_TAB_OVERVIEW : tabs.get(0).getId();
    }

    /**
     * Indicates this screen should not pause the game while open.
     *
     * @return always {@code false}
     */
    @Override
    public boolean isPauseScreen() { return false; }
}