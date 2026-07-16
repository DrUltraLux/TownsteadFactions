package com.drultralux.townsteadfactions.client.screen;

import com.drultralux.townsteadfactions.LogManager;
import com.drultralux.townsteadfactions.client.screen.widget.DraggableWidget;
import com.drultralux.townsteadfactions.client.screen.widget.ResourceDisplayWidget;
import com.drultralux.townsteadfactions.client.screen.widget.RosterDisplayWidget;
import com.drultralux.townsteadfactions.client.screen.widget.GlobalFactionsWidget;
import com.drultralux.townsteadfactions.config.ModConfig;
import dev.marie.MariesLib.client.GuiValueRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Centrally coordinates the multi-tab client dashboard panel interface.
 * Implements resizable layout boundaries, custom configurations, and automated storage flushes.
 */
public class FactionScreen extends Screen {
    /** Collection array tracking the dynamic view sheets designated as screen pages. */
    private final List<FactionTabPanel> tabPanels = new ArrayList<>();
    /** Internal custom string indices mapping the identity labels of active pages. */
    private final List<String> tabInternalIds = new ArrayList<>();

    /** The currently active focus tab page selection array index. */
    private int activeTabIndex = 0;
    /** Object state tracker pointing to the specific sub-window widget currently undergoing cursor movement. */
    private DraggableWidget activeDraggedComponent = null;

    /** The custom calculated pixel width dimension of the main housing panel frame container. */
    private int boxWidth;
    /** The custom calculated pixel height dimension of the main housing panel frame container. */
    private int boxHeight;
    /** Condition tracker tracking if the lower-right resize handle block is held down by the mouse. */
    private boolean isResizingBox = false;
    /** Horizontal vector coordinate offset tracking current mouse scaling adjustments. */
    private int resizeOffsetX;
    /** Vertical vector coordinate offset tracking current mouse scaling adjustments. */
    private int resizeOffsetY;

    /** Reference pointer to the synchronized treasury values window widget instance. */
    private ResourceDisplayWidget treasuryWidget;
    /** Reference pointer to the synchronized roster listing window widget instance. */
    private RosterDisplayWidget rosterWidget;
    /** Reference pointer to the synchronized world factions overview window widget instance. */
    private GlobalFactionsWidget globalWidget;

    /**
     * A single, reusable MariesLib functional instance handling all segment drawing loops.
     */
    private final GuiValueRenderer segmentRenderer = (graphics, x, y, level) -> {
        int filledBlocks = (int) (level * 10);
        for (int i = 0; i < 10; i++) {
            int color = (i < filledBlocks) ? 0xFF00FF00 : 0xFF2A2A2A;
            graphics.fill(x + (i * 8), y, x + (i * 8) + 6, y + 6, color);
        }
    };

    public FactionScreen() {
        super(Component.literal("Faction Dashboard"));
    }

    /**
     * Prepares configuration colors, injects overrides, and builds out tab layout panels.
     */
    @Override
    protected void init() {
        super.init();
        this.tabPanels.clear();
        this.tabInternalIds.clear();

        // 💡 AGNOSTIC ALIGNMENT: Uses the universal getters to pull dimension boundaries cleanly
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

        // 💡 AGNOSTIC ALIGNMENT: Uses the universal getters to translate coordinate snapshots cleanly
        this.treasuryWidget = new ResourceDisplayWidget(mainX + 20 + ModConfig.CLIENT.getInteger("treasuryWidgetX", -50), mainY + 40 + ModConfig.CLIENT.getInteger("treasuryWidgetY", -30));
        this.rosterWidget = new RosterDisplayWidget(mainX + 20 + ModConfig.CLIENT.getInteger("rosterWidgetX", 40), mainY + 40 + ModConfig.CLIENT.getInteger("rosterWidgetY", -10));
        this.globalWidget = new GlobalFactionsWidget(mainX + 20 + ModConfig.CLIENT.getInteger("globalWidgetX", -100), mainY + 40 + ModConfig.CLIENT.getInteger("globalWidgetY", 10));

        routeWidgetToTab(this.treasuryWidget, ModConfig.CLIENT.getInteger("treasuryWidgetTab", 0));
        routeWidgetToTab(this.rosterWidget, ModConfig.CLIENT.getInteger("rosterWidgetTab", 1));
        routeWidgetToTab(this.globalWidget, ModConfig.CLIENT.getInteger("globalWidgetTab", 2));
    }

    /**
     * Attaches a widget node straight to its target viewport tab layout container sheet array.
     */
    private void routeWidgetToTab(DraggableWidget widget, int preferredIndex) {
        if (preferredIndex >= 0 && preferredIndex < this.tabPanels.size()) {
            this.tabPanels.get(preferredIndex).addWidget(widget);
        } else {
            this.tabPanels.get(0).addWidget(widget);
        }
    }

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

        // Bypasses parent Screen.render background loops to prevent vanilla shader blur passes completely
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Leave completely blank! This physically destroys vanilla's post-processing blur shader hook loop.
    }

    /**
     * Formats and renders the custom buttons row layer across the top header bounds.
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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (com.drultralux.townsteadfactions.client.KeyMappings.OPEN_FACTION_DASHBOARD.matches(keyCode, scanCode) || keyCode == 256) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        flushPlacementsToConfig();
        super.onClose();
    }

    /**
     * Translates coordinates to relative metrics and flushes them down to configuration blocks.
     */
    private void flushPlacementsToConfig() {
        int mainX = this.width / 2 - (this.boxWidth / 2);
        int mainY = this.height / 2 - (this.boxHeight / 2);

        int tTab = findWidgetTabLocation(this.treasuryWidget);
        int rTab = findWidgetTabLocation(this.rosterWidget);
        int gTab = findWidgetTabLocation(this.globalWidget);

        ScreenLayoutSaver.saveWidgetLayout(
                this.treasuryWidget.getX() - mainX - 20, this.treasuryWidget.getY() - mainY - 40, tTab,
                this.rosterWidget.getX() - mainX - 20, this.rosterWidget.getY() - mainY - 40, rTab,
                this.globalWidget.getX() - mainX - 20, this.globalWidget.getY() - mainY - 40, gTab,
                this.boxWidth, this.boxHeight
        );
    }

    /**
     * Determines which tab index slot currently holds an active widget reference.
     */
    private int findWidgetTabLocation(DraggableWidget widget) {
        for (int i = 0; i < this.tabPanels.size(); i++) {
            if (this.tabPanels.get(i).getComponents().contains(widget)) return i;
        }
        return 0;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
