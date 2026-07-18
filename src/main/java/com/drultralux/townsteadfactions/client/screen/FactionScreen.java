package com.drultralux.townsteadfactions.client.screen;

import com.drultralux.townsteadfactions.client.KeyMappings;
import com.drultralux.townsteadfactions.client.screen.widget.ActivityLogWidget;
import com.drultralux.townsteadfactions.client.screen.widget.DraggableWidget;
import com.drultralux.townsteadfactions.client.screen.widget.GlobalFactionsWidget;
import com.drultralux.townsteadfactions.client.screen.widget.PlayerModelWidget;
import com.drultralux.townsteadfactions.client.screen.widget.ResourceDisplayWidget;
import com.drultralux.townsteadfactions.client.screen.widget.RosterDisplayWidget;
import com.drultralux.townsteadfactions.client.screen.widget.TabPanelWidget;
import com.drultralux.townsteadfactions.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * The main faction dashboard screen: a resizable, multi-tab window hosting
 * the treasury, roster, global factions, avatar, and activity log widgets.
 * Tab structure and widget placement are owned entirely by
 * {@link TabManager}; this screen only renders what it's given and
 * forwards input events into {@code TabManager}'s methods.
 */
public class FactionScreen extends Screen {

    /** The widget currently being dragged by the mouse, or {@code null} if none. */
    private DraggableWidget activeDraggedComponent = null;

    /** The current width of the main dashboard window, in pixels. */
    private int boxWidth;

    /** The current height of the main dashboard window, in pixels. */
    private int boxHeight;

    /** Whether the window's resize handle is currently being dragged. */
    private boolean isResizingBox = false;

    /** The mouse x offset captured when a resize drag begins. */
    private int resizeOffsetX;

    /** The mouse y offset captured when a resize drag begins. */
    private int resizeOffsetY;

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

        int mainX = this.width / 2 - (this.boxWidth / 2);
        int mainY = this.height / 2 - (this.boxHeight / 2);

        this.treasuryWidget = new ResourceDisplayWidget(mainX + 20 + ModConfig.CLIENT.getInteger("treasuryWidgetX", -50), mainY + 40 + ModConfig.CLIENT.getInteger("treasuryWidgetY", -30));
        this.rosterWidget = new RosterDisplayWidget(mainX + 20 + ModConfig.CLIENT.getInteger("rosterWidgetX", 40), mainY + 40 + ModConfig.CLIENT.getInteger("rosterWidgetY", -10));
        this.globalWidget = new GlobalFactionsWidget(mainX + 20 + ModConfig.CLIENT.getInteger("globalWidgetX", -100), mainY + 40 + ModConfig.CLIENT.getInteger("globalWidgetY", 10));
        this.avatarWidget = new PlayerModelWidget(mainX + 20 + ModConfig.CLIENT.getInteger("avatarWidgetX", 20), mainY + 40 + ModConfig.CLIENT.getInteger("avatarWidgetY", -10));
        this.activityWidget = new ActivityLogWidget(mainX + 20 + ModConfig.CLIENT.getInteger("activityWidgetX", -40), mainY + 40 + ModConfig.CLIENT.getInteger("activityWidgetY", 90));

        placeWidget(this.treasuryWidget, "treasuryWidgetTabId", TabManager.DEFAULT_TAB_OVERVIEW);
        placeWidget(this.rosterWidget, "rosterWidgetTabId", TabManager.DEFAULT_TAB_ROSTER);
        placeWidget(this.globalWidget, "globalWidgetTabId", TabManager.DEFAULT_TAB_GLOBAL);
        placeWidget(this.avatarWidget, "avatarWidgetTabId", TabManager.DEFAULT_TAB_OVERVIEW);
        placeWidget(this.activityWidget, "activityWidgetTabId", TabManager.DEFAULT_TAB_OVERVIEW);
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
     * Renders the dashboard background, window frame, resize handle, tab
     * headers, add-tab button, and the active tab's widgets.
     *
     * @param graphics the graphics context to draw with
     * @param mouseX the current mouse x position
     * @param mouseY the current mouse y position
     * @param partialTicks the partial tick time, for frame interpolation
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int overlayTopColor = 0xC0101010;
        int overlayBottomColor = 0xD00A0A0A;
        graphics.fillGradient(0, 0, this.width, this.height, overlayTopColor, overlayBottomColor);

        int mainX = this.width / 2 - (this.boxWidth / 2);
        int mainY = this.height / 2 - (this.boxHeight / 2);

        graphics.fill(mainX, mainY, mainX + this.boxWidth, mainY + this.boxHeight, 0xDD111111);
        graphics.renderOutline(mainX, mainY, this.boxWidth, this.boxHeight, 0xFF444444);

        int handleX = mainX + this.boxWidth - 8;
        int handleY = mainY + this.boxHeight - 8;
        graphics.fill(handleX, handleY, handleX + 6, handleY + 6, 0xFF666666);

        TabManager.layoutHeaders(mainX, mainY, this.font);
        String activeTabId = TabManager.getActiveTabId();
        boolean closeable = TabManager.getTabs().size() > 1;
        for (TabPanelWidget tab : TabManager.getTabs()) {
            if (!tab.getId().equals(this.renamingTabId)) {
                tab.renderHeader(graphics, this.font, tab.getId().equals(activeTabId), closeable);
            }
        }
        TabManager.renderAddButton(graphics, this.font);

        TabPanelWidget activeTab = TabManager.getActiveTab();
        if (activeTab != null) {
            for (DraggableWidget widget : activeTab.getComponents()) {
                int minWX = mainX + 4;
                if (widget.getX() < minWX) widget.setPosition(minWX, widget.getY());
                if (widget.getX() + 140 > mainX + this.boxWidth) widget.setPosition(mainX + this.boxWidth - 144, widget.getY());
                if (widget.getY() < mainY + 24) widget.setPosition(widget.getX(), mainY + 24);
                if (widget.getY() + 70 > mainY + this.boxHeight) widget.setPosition(widget.getX(), mainY + this.boxHeight - 74);
            }
            activeTab.renderContent(graphics, mouseX, mouseY, partialTicks);
        }

        // Render our own widgets (including an active rename EditBox, if any) directly,
        // bypassing the parent Screen's background/blur pass.
        for (Renderable renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTicks);
        }
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
     * Handles mouse clicks for resizing the window, adding a new tab, tab
     * header interaction (select or rename), and beginning a widget drag.
     * A click outside an active rename box commits that rename first.
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
            int mainX = this.width / 2 - (this.boxWidth / 2);
            int mainY = this.height / 2 - (this.boxHeight / 2);

            if (mouseX >= mainX + this.boxWidth - 10 && mouseX <= mainX + this.boxWidth && mouseY >= mainY + this.boxHeight - 10 && mouseY <= mainY + this.boxHeight) {
                this.isResizingBox = true;
                this.resizeOffsetX = (int) mouseX - this.boxWidth;
                this.resizeOffsetY = (int) mouseY - this.boxHeight;
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

            TabPanelWidget activeTab = TabManager.getActiveTab();
            if (activeTab != null) {
                for (DraggableWidget widget : activeTab.getComponents()) {
                    if (widget.mouseClicked(mouseX, mouseY, button)) {
                        this.activeDraggedComponent = widget;
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Handles dragging: either resizing the window, or moving the currently
     * dragged widget (and switching its tab if dropped over a different
     * tab header).
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
        if (this.isResizingBox) {
            this.boxWidth = Math.max(240, (int) mouseX - this.resizeOffsetX);
            this.boxHeight = Math.max(160, (int) mouseY - this.resizeOffsetY);
            return true;
        }

        if (this.activeDraggedComponent != null) {
            this.activeDraggedComponent.mouseDragged(mouseX, mouseY, button, dragX, dragY);

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
     * Ends any active resize or widget drag and persists the resulting
     * layout to the client config.
     *
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     * @param button the mouse button released
     * @return {@code true} if the release was handled
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.isResizingBox = false;
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
     * layout (widget positions/tabs, window size, and tab list) to the
     * client config.
     */
    private void flushPlacementsToConfig() {
        int mainX = this.width / 2 - (this.boxWidth / 2);
        int mainY = this.height / 2 - (this.boxHeight / 2);

        ScreenLayoutSaver.saveFullLayout(
                this.treasuryWidget.getX() - mainX - 20, this.treasuryWidget.getY() - mainY - 40, findWidgetTabId(this.treasuryWidget),
                this.rosterWidget.getX() - mainX - 20, this.rosterWidget.getY() - mainY - 40, findWidgetTabId(this.rosterWidget),
                this.globalWidget.getX() - mainX - 20, this.globalWidget.getY() - mainY - 40, findWidgetTabId(this.globalWidget),
                this.avatarWidget.getX() - mainX - 20, this.avatarWidget.getY() - mainY - 40, findWidgetTabId(this.avatarWidget),
                this.activityWidget.getX() - mainX - 20, this.activityWidget.getY() - mainY - 40, findWidgetTabId(this.activityWidget),
                this.boxWidth, this.boxHeight,
                TabManager.getTabs()
        );
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