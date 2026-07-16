package com.drultralux.townsteadfactions.client.screen;

import com.drultralux.townsteadfactions.LogManager;
import com.drultralux.townsteadfactions.client.screen.widget.DraggableWidget;
import com.drultralux.townsteadfactions.client.screen.widget.ResourceDisplayWidget;
import com.drultralux.townsteadfactions.client.screen.widget.RosterDisplayWidget;
import com.drultralux.townsteadfactions.config.ModConfig;
import dev.marie.MariesLib.client.GuiValueRenderer;
import dev.marie.MariesLib.client.MarieValueColors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * High-performance client-side dashboard panel for managing faction data.
 * Manages multiple sub-page panel viewports and routes mouse interaction inputs dynamically.
 */
public class FactionScreen extends Screen {
    private final List<FactionTabPanel> tabPanels = new ArrayList<>();
    private int activeTabIndex = 0;
    private DraggableWidget activeDraggedComponent = null;

    private final GuiValueRenderer segmentRenderer = (graphics, x, y, level) -> {
        int filledBlocks = (int) (level * 10);
        // Simplified rendering loop logic
        for (int i = 0; i < 10; i++) {
            graphics.fill(x + (i * 8), y, x + (i * 8) + 6, y + 6, (i < filledBlocks) ? 0xFFFFFFFF : 0xFF2A2A2A);
        }
    };

    public FactionScreen() {
        super(Component.literal("Faction Dashboard"));
        LogManager.debug("Instantiating faction framework workspace view...");
    }

    @Override
    protected void init() {
        super.init();
        this.tabPanels.clear();

        // 1. Set up the three primary page tabs using clean component structures
        FactionTabPanel overviewTab = new FactionTabPanel(Component.literal("Overview"));
        FactionTabPanel rosterTab = new FactionTabPanel(Component.literal("Roster"));
        FactionTabPanel globalTab = new FactionTabPanel(Component.literal("Global"));

        this.tabPanels.add(overviewTab);
        this.tabPanels.add(rosterTab);
        this.tabPanels.add(globalTab);

        // 2. Read persistent layout metrics right out of the client configuration files
        int savedX = ModConfig.CLIENT.treasuryWidgetX.get();
        int savedY = ModConfig.CLIENT.treasuryWidgetY.get();
        int savedTabIdx = ModConfig.CLIENT.treasuryWidgetTab.get();

        // 3. Create our widgets and insert them into their respective tabs
        ResourceDisplayWidget treasuryWidget = new ResourceDisplayWidget(savedX, savedY);
        if (savedTabIdx >= 0 && savedTabIdx < this.tabPanels.size()) {
            this.tabPanels.get(savedTabIdx).addWidget(treasuryWidget);
        } else {
            overviewTab.addWidget(treasuryWidget);
        }

        // Default the Roster display box straight onto its primary social tab page
        RosterDisplayWidget activeRosterWidget = new RosterDisplayWidget(this.width / 2 - 80, this.height / 2 - 60);
        rosterTab.addWidget(activeRosterWidget);

        this.activeTabIndex = (savedTabIdx >= 0 && savedTabIdx < this.tabPanels.size()) ? savedTabIdx : 0;
        LogManager.debug("Screen elements bound to active viewports successfully.");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Draw the background canvas elements cleanly
        this.renderBackground(graphics, mouseX, mouseY, partialTicks);

        // Draw basic outer frame backing container
        int mainX = this.width / 2 - 180;
        int mainY = this.height / 2 - 110;
        graphics.fill(mainX, mainY, mainX + 360, mainY + 220, 0xDD111111);
        graphics.renderOutline(mainX, mainY, 360, 220, 0xFF444444);

        // Render tab button selection headers
        renderTabHeaders(graphics, mouseX, mouseY);

        // Render the contents of our active tab layout sheet natively
        if (this.activeTabIndex >= 0 && this.activeTabIndex < this.tabPanels.size()) {
            this.tabPanels.get(this.activeTabIndex).renderContents(graphics, mouseX, mouseY, partialTicks, this.segmentRenderer);
        }
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void renderTabHeaders(GuiGraphics graphics, int mouseX, int mouseY) {
        int tabX = this.width / 2 - 160;
        int tabY = this.height / 2 - 102;

        for (int i = 0; i < this.tabPanels.size(); i++) {
            FactionTabPanel panel = this.tabPanels.get(i);
            int textW = this.font.width(panel.getTabTitle());

            // Highlight active selection tabs clearly
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
            int tabX = this.width / 2 - 160;
            int tabY = this.height / 2 - 102;

            for (int i = 0; i < this.tabPanels.size(); i++) {
                int textW = this.font.width(this.tabPanels.get(i).getTabTitle());
                if (mouseX >= tabX - 4 && mouseX <= tabX + textW + 4 && mouseY >= tabY - 2 && mouseY <= tabY + 12) {
                    this.activeTabIndex = i;
                    LogManager.debug("Focus switched to tab index: " + i);
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
        if (this.activeDraggedComponent != null) {
            this.activeDraggedComponent.mouseDragged(mouseX, mouseY, button, dragX, dragY);

            int tabX = this.width / 2 - 160;
            int tabY = this.height / 2 - 102;

            for (int i = 0; i < this.tabPanels.size(); i++) {
                FactionTabPanel panel = this.tabPanels.get(i);
                int textW = this.font.width(panel.getTabTitle());
                if (mouseX >= tabX - 4 && mouseX <= tabX + textW + 4 && mouseY >= tabY - 2 && mouseY <= tabY + 12) {
                    if (this.activeTabIndex != i) {
                        this.tabPanels.get(this.activeTabIndex).removeWidget(this.activeDraggedComponent);
                        panel.addWidget(this.activeDraggedComponent);
                        this.activeTabIndex = i;
                        LogManager.debug("Drifted widget layout tracking context to tab index: " + i);
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
        if (this.activeDraggedComponent != null) {
            this.activeDraggedComponent.mouseReleased(mouseX, mouseY, button);

            ScreenLayoutSaver.saveTreasuryPosition(
                    this.activeDraggedComponent.getX(),
                    this.activeDraggedComponent.getY(),
                    this.activeTabIndex
            );

            this.activeDraggedComponent = null;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}