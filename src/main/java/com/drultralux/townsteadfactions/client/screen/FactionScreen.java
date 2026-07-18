package com.drultralux.townsteadfactions.client.screen;

import com.drultralux.townsteadfactions.client.KeyMappings;
import com.drultralux.townsteadfactions.client.screen.widget.ActivityLogWidget;
import com.drultralux.townsteadfactions.client.screen.widget.DraggableWidget;
import com.drultralux.townsteadfactions.client.screen.widget.GlobalFactionsWidget;
import com.drultralux.townsteadfactions.client.screen.widget.PlayerModelWidget;
import com.drultralux.townsteadfactions.client.screen.widget.ResourceDisplayWidget;
import com.drultralux.townsteadfactions.client.screen.widget.RosterDisplayWidget;
import com.drultralux.townsteadfactions.config.ModConfig;
import dev.marie.MariesLib.client.GuiValueRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The main faction dashboard screen: a resizable, multi-tab window hosting
 * the treasury, roster, global factions, avatar, and activity log widgets.
 * Widget positions and window size are persisted to the client config.
 */
public class FactionScreen extends Screen {

    /** The tab panels making up this screen, in display order. */
    private final List<FactionTabPanel> tabPanels = new ArrayList<>();

    /** Internal identifiers for each tab, parallel to {@link #tabPanels}. */
    private final List<String> tabInternalIds = new ArrayList<>();

    /** The index of the currently selected tab. */
    private int activeTabIndex = 0;

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

    /**
     * A shared bar-segment renderer passed to tab panels for drawing
     * resource levels.
     */
    private final GuiValueRenderer segmentRenderer = (graphics, x, y, level) -> {
        int filledBlocks = (int) (level * 10);
        for (int i = 0; i < 10; i++) {
            int color = (i < filledBlocks) ? 0xFF00FF00 : 0xFF2A2A2A;
            graphics.fill(x + (i * 8), y, x + (i * 8) + 6, y + 6, color);
        }
    };

    /**
     * Creates the faction dashboard screen.
     */
    public FactionScreen() {
        super(Component.literal("Faction Dashboard"));
    }

    /**
     * Builds the screen's tabs and widgets from the client configuration,
     * falling back to defaults if no tab layout is configured.
     */
    @Override
    protected void init() {
        super.init();
        this.tabPanels.clear();
        this.tabInternalIds.clear();

        this.boxWidth = ModConfig.CLIENT.getInteger("mainBoxWidth", 360);
        this.boxHeight = ModConfig.CLIENT.getInteger("mainBoxHeight", 220);

        List<String> tabMappings = ModConfig.CLIENT.getStringList("customizedTabOrder");
        for (String mapping : tabMappings) {
            if (mapping == null || !mapping.contains(";")) continue;
            String[] tokens = mapping.split(";");
            if (tokens.length >= 2) {
                this.tabInternalIds.add(tokens[0]);
                this.tabPanels.add(new FactionTabPanel(Component.literal(tokens[1])));
            }
        }

        if (this.tabPanels.isEmpty()) {
            this.tabInternalIds.add("OVERVIEW"); this.tabPanels.add(new FactionTabPanel(Component.literal("Overview")));
            this.tabInternalIds.add("ROSTER"); this.tabPanels.add(new FactionTabPanel(Component.literal("Roster")));
            this.tabInternalIds.add("GLOBAL"); this.tabPanels.add(new FactionTabPanel(Component.literal("Global")));
        }

        int mainX = this.width / 2 - (this.boxWidth / 2);
        int mainY = this.height / 2 - (this.boxHeight / 2);

        this.treasuryWidget = new ResourceDisplayWidget(mainX + 20 + ModConfig.CLIENT.getInteger("treasuryWidgetX", -50), mainY + 40 + ModConfig.CLIENT.getInteger("treasuryWidgetY", -30));
        this.rosterWidget = new RosterDisplayWidget(mainX + 20 + ModConfig.CLIENT.getInteger("rosterWidgetX", 40), mainY + 40 + ModConfig.CLIENT.getInteger("rosterWidgetY", -10));
        this.globalWidget = new GlobalFactionsWidget(mainX + 20 + ModConfig.CLIENT.getInteger("globalWidgetX", -100), mainY + 40 + ModConfig.CLIENT.getInteger("globalWidgetY", 10));
        this.avatarWidget = new PlayerModelWidget(mainX + 20 + ModConfig.CLIENT.getInteger("avatarWidgetX", 20), mainY + 40 + ModConfig.CLIENT.getInteger("avatarWidgetY", -10));
        this.activityWidget = new ActivityLogWidget(mainX + 20 + ModConfig.CLIENT.getInteger("activityWidgetX", -40), mainY + 40 + ModConfig.CLIENT.getInteger("activityWidgetY", 90));

        routeWidgetToTab(this.treasuryWidget, ModConfig.CLIENT.getInteger("treasuryWidgetTab", 0));
        routeWidgetToTab(this.rosterWidget, ModConfig.CLIENT.getInteger("rosterWidgetTab", 1));
        routeWidgetToTab(this.globalWidget, ModConfig.CLIENT.getInteger("globalWidgetTab", 2));
        routeWidgetToTab(this.avatarWidget, ModConfig.CLIENT.getInteger("avatarWidgetTab", 0));
        routeWidgetToTab(this.activityWidget, ModConfig.CLIENT.getInteger("activityWidgetTab", 0));
    }

    /**
     * Adds a widget to the tab at the given index, or to the first tab if
     * the index is out of range.
     *
     * @param widget the widget to add
     * @param preferredIndex the index of the tab to add it to
     */
    private void routeWidgetToTab(DraggableWidget widget, int preferredIndex) {
        if (preferredIndex >= 0 && preferredIndex < this.tabPanels.size()) {
            this.tabPanels.get(preferredIndex).addWidget(widget);
        } else {
            this.tabPanels.get(0).addWidget(widget);
        }
    }

    /**
     * Renders the dashboard background, window frame, resize handle, tab
     * headers, and the active tab's widgets.
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

        renderTabHeaders(graphics, mainX, mainY);

        if (this.activeTabIndex >= 0 && this.activeTabIndex < this.tabPanels.size()) {
            for (DraggableWidget widget : this.tabPanels.get(this.activeTabIndex).getComponents()) {
                int minWX = mainX + 4;
                if (widget.getX() < minWX) widget.setPosition(minWX, widget.getY());
                if (widget.getX() + 140 > mainX + this.boxWidth) widget.setPosition(mainX + this.boxWidth - 144, widget.getY());
                if (widget.getY() < mainY + 24) widget.setPosition(widget.getX(), mainY + 24);
                if (widget.getY() + 70 > mainY + this.boxHeight) widget.setPosition(widget.getX(), mainY + this.boxHeight - 74);
            }
            this.tabPanels.get(this.activeTabIndex).renderContents(graphics, mouseX, mouseY, partialTicks, this.segmentRenderer);
        }

        // Render our own widgets directly, bypassing the parent Screen's background/blur pass.
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
     * Renders the row of tab header buttons across the top of the window.
     *
     * @param graphics the graphics context to draw with
     * @param mainX the x position of the main window
     * @param mainY the y position of the main window
     */
    private void renderTabHeaders(GuiGraphics graphics, int mainX, int mainY) {
        int tabX = mainX + 20;
        int tabY = mainY + 8;

        for (int i = 0; i < this.tabPanels.size(); i++) {
            FactionTabPanel panel = this.tabPanels.get(i);
            int textW = this.font.width(panel.getTabTitle());
            int tabColor = (i == this.activeTabIndex) ? 0xFFFFFFFF : 0xAAAAAA;

            graphics.fill(tabX - 4, tabY - 2, tabX + textW + 4, tabY + 12, (i == this.activeTabIndex) ? 0xFF333333 : 0xFF222222);
            graphics.renderOutline(tabX - 4, tabY - 2, textW + 8, 14, 0xFF555555);
            graphics.drawString(this.font, panel.getTabTitle(), tabX, tabY + 1, tabColor, false);

            tabX += textW + 24;
        }
    }

    /**
     * Handles mouse clicks for resizing the window, switching tabs, and
     * beginning a widget drag.
     *
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     * @param button the mouse button pressed
     * @return {@code true} if the click was handled
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mainX = this.width / 2 - (this.boxWidth / 2);
            int mainY = this.height / 2 - (this.boxHeight / 2);

            if (mouseX >= mainX + this.boxWidth - 10 && mouseX <= mainX + this.boxWidth && mouseY >= mainY + this.boxHeight - 10 && mouseY <= mainY + this.boxHeight) {
                this.isResizingBox = true;
                this.resizeOffsetX = (int) mouseX - this.boxWidth;
                this.resizeOffsetY = (int) mouseY - this.boxHeight;
                return true;
            }

            int tabX = mainX + 20;
            int tabY = mainY + 8;
            for (int i = 0; i < this.tabPanels.size(); i++) {
                int textW = this.font.width(this.tabPanels.get(i).getTabTitle());
                if (mouseX >= tabX - 4 && mouseX <= tabX + textW + 4 && mouseY >= tabY - 2 && mouseY <= tabY + 12) {
                    this.activeTabIndex = i;
                    return true;
                }
                tabX += textW + 24;
            }

            if (this.activeTabIndex >= 0 && this.activeTabIndex < this.tabPanels.size()) {
                for (DraggableWidget widget : this.tabPanels.get(this.activeTabIndex).getComponents()) {
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

            int mainX = this.width / 2 - (this.boxWidth / 2);
            int mainY = this.height / 2 - (this.boxHeight / 2);
            int tabX = mainX + 20;
            int tabY = mainY + 8;

            for (int i = 0; i < this.tabPanels.size(); i++) {
                FactionTabPanel panel = this.tabPanels.get(i);
                int textW = this.font.width(panel.getTabTitle());
                if (mouseX >= tabX - 4 && mouseX <= tabX + textW + 4 && mouseY >= tabY - 2 && mouseY <= tabY + 12) {
                    if (this.activeTabIndex != i) {
                        this.tabPanels.get(this.activeTabIndex).removeWidget(this.activeDraggedComponent);
                        panel.addWidget(this.activeDraggedComponent);
                        this.activeTabIndex = i;
                        break;
                    }
                }
                tabX += textW + 24;
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
     * Closes the dashboard when the dashboard hotkey or Escape is pressed.
     *
     * @param keyCode the key code pressed
     * @param scanCode the platform-specific scan code
     * @param modifiers the modifier key bitmask
     * @return {@code true} if the key press was handled
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (KeyMappings.OPEN_FACTION_DASHBOARD.matches(keyCode, scanCode) || keyCode == 256) {
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
     * Converts each widget's absolute position to a position relative to
     * the window, and its current tab, then saves them all to the client
     * config.
     */
    private void flushPlacementsToConfig() {
        int mainX = this.width / 2 - (this.boxWidth / 2);
        int mainY = this.height / 2 - (this.boxHeight / 2);

        int tTab = findWidgetTabLocation(this.treasuryWidget);
        int rTab = findWidgetTabLocation(this.rosterWidget);
        int gTab = findWidgetTabLocation(this.globalWidget);
        int aTab = findWidgetTabLocation(this.avatarWidget);
        int logTab = findWidgetTabLocation(this.activityWidget);

        ScreenLayoutSaver.saveWidgetLayout(
                this.treasuryWidget.getX() - mainX - 20, this.treasuryWidget.getY() - mainY - 40, tTab,
                this.rosterWidget.getX() - mainX - 20, this.rosterWidget.getY() - mainY - 40, rTab,
                this.globalWidget.getX() - mainX - 20, this.globalWidget.getY() - mainY - 40, gTab,
                this.avatarWidget.getX() - mainX - 20, this.avatarWidget.getY() - mainY - 40, aTab,
                this.activityWidget.getX() - mainX - 20, this.activityWidget.getY() - mainY - 40, logTab,

                this.boxWidth, this.boxHeight
        );
    }

    /**
     * Finds which tab currently contains the given widget.
     *
     * @param widget the widget to locate
     * @return the index of the tab containing it, or {@code 0} if not found
     */
    private int findWidgetTabLocation(DraggableWidget widget) {
        for (int i = 0; i < this.tabPanels.size(); i++) {
            if (this.tabPanels.get(i).getComponents().contains(widget)) return i;
        }
        return 0;
    }

    /**
     * Indicates this screen should not pause the game while open.
     *
     * @return always {@code false}
     */
    @Override
    public boolean isPauseScreen() { return false; }
}