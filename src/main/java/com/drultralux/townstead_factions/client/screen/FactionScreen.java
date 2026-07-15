package com.drultralux.townstead_factions.client.screen;

import com.drultralux.townstead_factions.client.ClientFactionCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class FactionScreen extends Screen {

    private enum Tab { OVERVIEW, ROSTER, GLOBAL, SETTINGS }
    private Tab activeTab = Tab.OVERVIEW;
    private static int overviewScrollAmount = 0;

    private static int frameX = -1;
    private static int frameY = -50;
    private static int frameW = 320;
    private static int frameH = 220;

    private boolean isDraggingFrame = false;
    private boolean isResizingFrame = false;
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;

    private static final int MIN_HEIGHT = 150;
    private static final int MAX_WIDTH = 450;
    private static final int TITLE_BAR_HEIGHT = 16;
    private static final int RESIZE_ZONE_SIZE = 10;

    private boolean isDraggingScrollbar = false;

    public FactionScreen() {
        super(Component.literal("Faction Menu"));
    }

    @Override
    protected void init() {
        super.init();

        PacketDistributor.sendToServer(new com.drultralux.townstead_factions.client.MenuRequestPayload());

        if (frameX == -1) {
            this.frameX = (this.width - this.frameW) / 2;
            this.frameY = (this.height - this.frameH) / 2;
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics, mouseX, mouseY, partialTicks);

        // Render main bounding background box canvas grid framework
        graphics.fill(frameX, frameY, frameX + frameW, frameY + frameH, 0xFF121212);

        renderTopTabs(graphics, mouseX, mouseY);

        int contentX = frameX + 15;
        int contentY = frameY + 30;
        int contentW = frameW - 30;
        int contentH = frameH - 45;

        // UNIFIED VIEWPORT SCISSOR MASK LAYER
        graphics.enableScissor(contentX, contentY, contentX + contentW, contentY + contentH);
        graphics.pose().pushPose();

        // Applies vertical scrolling adjustments to ALL subcomponents simultaneously
        graphics.pose().translate(0.0F, (float) -overviewScrollAmount, 0.0F);

        if (activeTab == Tab.OVERVIEW) {
            // Draw relative banner layout
            int relativeX = contentX;
            int relativeY = contentY;
            int boxW = 55;
            int boxH = 75;
            int bannerBoxY = relativeY;
            int bannerH = 14;
            int playerBoxY = bannerBoxY + bannerH + 4;

            int bannerSquareSize = bannerH + 4;
            int bannerStartX = relativeX + (boxW - bannerSquareSize) / 2;
            graphics.fill(bannerStartX, bannerBoxY - 2, bannerStartX + bannerSquareSize, bannerBoxY + bannerH + 2, 0xFF7A2222);

            // Draw relative active overview texts
            renderOverviewTab(graphics, contentX, contentY, mouseX, mouseY + overviewScrollAmount);

            //DRAW UNIFIED 3D AVATAR MODEL (Linked cleanly to real-time scroll vectors)
            if (this.minecraft != null && this.minecraft.player != null) {
                Quaternionf entityRotation = new Quaternionf().rotationX((float) Math.PI);
                Quaternionf cameraOrientation = new Quaternionf().rotationXYZ(0.0F, 0.0F, 0.0F);
                InventoryScreen.renderEntityInInventory(
                        graphics,
                        (float) (relativeX + (boxW / 2) - 3),
                        (float) (playerBoxY + boxH - 6),
                        30,
                        new Vector3f(0.0F, 0.0F, 0.0F),
                        entityRotation,
                        cameraOrientation,
                        (net.minecraft.world.entity.LivingEntity) this.minecraft.player
                );
            }
        } else if (activeTab == Tab.ROSTER) {
            renderRosterTab(graphics, contentX, contentY);
        } else if (activeTab == Tab.GLOBAL) {
            renderGlobalFactionsTab(graphics, contentX, contentY);
        } else if (activeTab == Tab.SETTINGS) {
            renderSettingsTab(graphics, contentX, contentY);
        }

        graphics.pose().popPose();
        graphics.disableScissor();

        // Render scroll track elements inside the static frame coordinates zone
        if (activeTab == Tab.OVERVIEW) {
            int scrollTrackX = frameX + frameW - 10;
            int scrollTrackY = contentY;
            graphics.fill(scrollTrackX, scrollTrackY, scrollTrackX + 6, scrollTrackY + contentH, 0xFF0A0A0A);

            int totalContentHeightNeeded = 220;
            int maxScroll = Math.max(0, totalContentHeightNeeded - contentH);
            if (maxScroll > 0) {
                int thumbH = Math.max(10, (contentH * contentH) / totalContentHeightNeeded);
                int thumbY = scrollTrackY + (overviewScrollAmount * (contentH - thumbH)) / maxScroll;
                graphics.fill(scrollTrackX + 1, thumbY, scrollTrackX + 5, thumbY + thumbH, 0xFF555555);
            }
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void renderTopTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        String[] tabLabels = {"Overview", "Roster", "Global", "Settings"};
        int count = tabLabels.length;

        // Dynamically scales the widths of navigation buttons to match whatever size you stretch the screen frame to!
        int availableWidth = frameW - 12;
        int tabW = (availableWidth / count) - 2;
        int tabH = 14;
        int startX = frameX + 6;
        int startY = frameY + 8;

        for (int i = 0; i < count; i++) {
            int currentX = startX + (i * (tabW + 2));
            boolean isCurrent = activeTab.ordinal() == i;
            boolean isHovered = mouseX >= currentX && mouseX <= currentX + tabW && mouseY >= startY && mouseY <= startY + tabH;

            int tabColor = isCurrent ? 0xFF3D3D3D : (isHovered ? 0xFF2E2E2E : 0xFF151515);
            int borderColor = isCurrent ? 0xFF606060 : 0xFF0D0D0D;

            graphics.fill(currentX, startY, currentX + tabW, startY + tabH, borderColor);
            graphics.fill(currentX + 1, startY + 1, currentX + tabW - 1, startY + tabH - 1, tabColor);

            String label = tabLabels[i];
            int textW = this.font.width(label);
            int labelX = currentX + (tabW - textW) / 2;
            int textColor = isCurrent ? 0xFFE5C158 : 0xA0A0A0; // Active gold selection color maps

            graphics.drawString(this.font, label, labelX, startY + 3, textColor, false);
        }
    }

    private void renderOverviewTab(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        int contentW = frameW - 30;
        int topRowH = 82;

        // Column 1: Members/Power Data Column
        int col1X = x + 80;
        graphics.drawString(this.font, "§6§l" + ClientFactionCache.getCurrentFaction().toUpperCase(), col1X, y + 2, 0xFFFFFF);
        graphics.drawString(this.font, "Title: " + ClientFactionCache.getCurrentCleanOrigin() + " (Leader)", col1X, y + 14, 0xAAAAAA);
        graphics.drawString(this.font, "Power: " + ClientFactionCache.getFactionSize() + " / " + ClientFactionCache.getTotalGlobalPlayers(), col1X, y + 26, 0xAAAAAA);

        // Render Segmented Member Squares Grid (1 Filled, 11 Empty)
        int activeSize = ClientFactionCache.getFactionSize();
        int totalGlobal = ClientFactionCache.getTotalGlobalPlayers();

        // Calculate the clean fractional ratio threshold (Default to 0 if no players are logged in)
        double ratio = (totalGlobal > 0) ? (double) activeSize / totalGlobal : 0.0;
        int filledBoxesNeeded = (int) Math.round(ratio * 10.0);

        int blockX = col1X;
        for (int b = 0; b < 10; b++) {
            // If the current box position sits within the percentage ratio, light it up red!
            int blockColor = (b < filledBoxesNeeded) ? 0xFFCC2222 : 0xFF2A2A2A;
            graphics.fill(blockX, y + 38, blockX + 6, y + 44, blockColor);
            blockX += 8;
        }

        graphics.drawString(this.font, "§d§lAIRSHIPS", col1X, y + 52, 0xFFFFFF);

        // Render Segmented Power Squares Grid (9 Filled, 1 Empty)
        int pBlockX = col1X;
        for (int p = 0; p < 10; p++) {
            int blockColor = (p < 9) ? 0xFFD63384 : 0xFF2A2A2A; // Pink for filled, Dark Grey for empty
            graphics.fill(pBlockX, y + 64, pBlockX + 6, y + 70, blockColor);
            pBlockX += 8;
        }

        // Column 2: Statistics & Storage Column
        int col2X = x + 200;
        graphics.drawString(this.font, "§6§lRESOURCES", col2X, y + 2, 0xFFFFFF);
        graphics.drawString(this.font, "• Cogs:", col2X, y + 14, 0xAAAAAA);

        //Currency Squares
        int currentCogs = ClientFactionCache.getCogs();
        int gBlockX = col2X;
        for (int g = 0; g < 10; g++) {
            // Fills blocks up to your stored count value (assuming 1 block per 1 unit up to 10 max)
            int blockColor = (g < currentCogs) ? 0xFFFFAA00 : 0xFF2A2A2A;
            graphics.fill(gBlockX, y + 26, gBlockX + 6, y + 32, blockColor);
            gBlockX += 8;
        }

        graphics.drawString(this.font, "• Food Supplies:", col2X, y + 40, 0xAAAAAA);

        //Food Squares
        int currentFood = ClientFactionCache.getFood();
        int iBlockX = col2X;
        for (int i = 0; i < 10; i++) {
            int blockColor = (i < currentFood) ? 0xFF55FF55 : 0xFF2A2A2A;
            graphics.fill(iBlockX, y + 52, iBlockX + 6, y + 58, blockColor);
            iBlockX += 8;
        }

        graphics.drawString(this.font, "• Mana Stockpile:", col2X, y + 66, 0xAAAAAA);

        //Magic Squares
        int currentMana = ClientFactionCache.getMana();
        int lBlockX = col2X;
        for (int l = 0; l < 10; l++) {
            int blockColor = (l < currentMana) ? 0xFF00AAAA : 0xFF2A2A2A;
            graphics.fill(lBlockX, y + 78, lBlockX + 6, y + 84, blockColor);
            lBlockX += 8;
        }

        int extendedTopRowH = 92; // Expanded by 10 pixels to comfortably house the Lapis row

        // Horizontal Separation Border Rule
        graphics.fill(x, y + extendedTopRowH + 2, x + contentW, y + extendedTopRowH + 3, 0xFF2D2D2D);

        //Activity Log Box Viewport Layout
        int logY = y + extendedTopRowH + 10;
        int logW = frameW - 30;
        int logH = 90;

        graphics.drawString(this.font, "§6⚙ACTIVITY LOG", x, logY, 0xFFFFFF);
        int innerBoxY = logY + 14;
        graphics.fill(x, innerBoxY, x + logW, innerBoxY + logH, 0xFF0A0A0A);

        String[] logLines = {
                "• DrUltraLux opened the interface menu screen.",
                "• Server data synchronization payload channel verified cleanly.",
                "• Local core faction capability sheet maps initialized.",
                "• Database sector checks returning successful status blocks.",
                "• Active Townstead registry configurations parsed into memory layers."
        };

        int totalRows = logLines.length;
        int rowOffset = 6;
        for (int i = 0; i < totalRows; i++) {
            graphics.drawString(this.font, logLines[i], x + 4, innerBoxY + rowOffset, 0xAAAAAA);
            rowOffset += 12;
        }
    }

    private void renderRosterTab(GuiGraphics graphics, int x, int y) {
        graphics.drawString(this.font, "§6§lONLINE ROSTER", x, y, 0xFFFFFF);
        List<String> members = ClientFactionCache.getActiveRoster();
        if (members == null || members.isEmpty()) {
            graphics.drawString(this.font, "§7No other online companions found.", x, y + 20, 0xFFFFFF);
        } else {
            int offset = 20;
            for (int i = 0; i < Math.min(members.size(), (frameH - 60) / 12); i++) {
                graphics.drawString(this.font, "§a• §7" + members.get(i), x + 4, y + offset, 0xFFFFFF);
                offset += 12;
            }
        }
    }

    private void renderGlobalFactionsTab(GuiGraphics graphics, int x, int y) {
        graphics.drawString(this.font, "§6§lWORLD FACTIONS", x, y, 0xFFFFFF);

        List<String> activeFactionNames = ClientFactionCache.getDiscoveredFactions();

        if (activeFactionNames == null || activeFactionNames.isEmpty()) {
            graphics.drawString(this.font, "§7No custom factions received from server.", x, y + 20, 0xFFFFFF);
        } else {
            int offset = 20;
            int counter = 1;

            for (String factionName : activeFactionNames) {
                graphics.drawString(this.font, "§7" + counter + ". §e" + factionName + " Faction", x + 4, y + offset, 0xFFFFFF);
                offset += 14;
                counter++;
                if (counter > 8) break;
            }
        }
    }

    private void renderSettingsTab(GuiGraphics graphics, int x, int y) {
        graphics.drawString(this.font, "§6§lFACTION SETTINGS", x, y, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {

            if (activeTab == Tab.OVERVIEW) {
                int contentH = frameH - TITLE_BAR_HEIGHT - 35;
                int scrollTrackX = frameX + frameW - 10;
                if (mouseX >= scrollTrackX && mouseX <= scrollTrackX + 6 && mouseY >= (frameY + TITLE_BAR_HEIGHT + 20) && mouseY <= (frameY + TITLE_BAR_HEIGHT + 20) + contentH) {
                    this.isDraggingScrollbar = true;
                    return true;
                }
            }

            if (mouseX >= (frameX + frameW - RESIZE_ZONE_SIZE) && mouseX <= (frameX + frameW) &&
                    mouseY >= (frameY + frameH - RESIZE_ZONE_SIZE) && mouseY <= (frameY + frameH)) {
                this.isResizingFrame = true;
                return true;
            }

            if (mouseX >= frameX && mouseX <= (frameX + frameW) &&
                    mouseY >= frameY && mouseY <= (frameY + 8)) {
                this.isDraggingFrame = true;
                this.dragOffsetX = mouseX - frameX;
                this.dragOffsetY = mouseY - frameY;
                return true;
            }

            int availableWidth = frameW - 12;
            int tabW = (availableWidth / Tab.values().length) - 2;
            int tabH = 14;
            int startX = frameX + 6;
            int startY = frameY + 8;

            if (mouseY >= startY && mouseY <= startY + tabH) {
                for (int i = 0; i < Tab.values().length; i++) {
                    int currentX = startX + (i * (tabW + 2));
                    if (mouseX >= currentX && mouseX <= currentX + tabW) {
                        this.activeTab = Tab.values()[i];
                        Minecraft.getInstance().getSoundManager().play(
                                SimpleSoundInstance.forUI(
                                        SoundEvents.UI_BUTTON_CLICK, 1.0F
                                )
                        );
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0) {

            if (this.isDraggingScrollbar && activeTab == Tab.OVERVIEW) {
                int contentY = frameY + 30;
                int contentH = frameH - 45;
                int totalNeededH = 210;
                int maxScroll = Math.max(0, totalNeededH - contentH);

                double relativeY = Math.clamp(mouseY - contentY, 0, contentH);
                overviewScrollAmount = (int) ((relativeY / (double)contentH) * maxScroll);
                return true;
            }

            //Process frame expansion resizing operations in real-time
            if (this.isResizingFrame) {
                int calculatedW = (int) (mouseX - this.frameX);
                int calculatedH = (int) (mouseY - this.frameY);

                int currentMinWidth = getDynamicMinWidth();
                this.frameW = Math.clamp(calculatedW, currentMinWidth, MAX_WIDTH);
                this.frameH = Math.max(calculatedH, MIN_HEIGHT);
                return true;
            }

            //Shift the panel's absolute screen coordinates based on hand movements
            if (this.isDraggingFrame) {
                int nextX = (int) (mouseX - this.dragOffsetX);
                this.frameX = Math.clamp(nextX, 0, Math.max(0, this.width - this.frameW));
                this.frameY = (int) (mouseY - dragOffsetY);
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {

            this.isDraggingScrollbar = false;

            // Drop tracking states instantly when mouse clicks release
            this.isDraggingFrame = false;
            this.isResizingFrame = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        //Safely terminates the fullscreen dark vignette overlay shader pass!
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Retrieve the current physical GLFW key integer bound to your custom binding configuration
        int targetToggleKey = KeyMappings.OPEN_FACTION_MENU.getKey().getValue();

        if (keyCode == targetToggleKey) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.closeContainer();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    //come back to this later
    private void drawProgressGridBlocks(GuiGraphics graphics, int x, int y, int filledCount, int totalCount, int blockColor) {
        int blockW = 6;
        int blockH = 8;
        int spacing = 2;

        for (int i = 0; i < totalCount; i++) {
            int currentX = x + (i * (blockW + spacing));
            // Filled blocks use the specified color; empty slots default to a deep dark base tint
            int color = (i < filledCount) ? blockColor : 0xFF141414;

            // Draw individual status segment blocks
            graphics.fill(currentX, y, currentX + blockW, y + blockH, 0xFF000000); // Outer edge border
            graphics.fill(currentX + 1, y + 1, currentX + blockW - 1, y + blockH - 1, color);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (activeTab == Tab.OVERVIEW) {
            // Calculate max scroll based on total content height
            int maxScroll = Math.max(0, 210 - (frameH - TITLE_BAR_HEIGHT - 35));

            overviewScrollAmount = Math.clamp((int)(overviewScrollAmount - (scrollY * 16)), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    //Needs a bit of rework...
    private int getDynamicMinWidth() {
        int baseMargin = 30;
        switch (activeTab) {
            case OVERVIEW -> {
                // Measure both columns to see which string is currently the longest layout asset
                int leftColWidth = 80 + this.font.width("Title: " + ClientFactionCache.getCurrentRawRootID() + " (Leader)");
                int rightColWidth = 200 + this.font.width("§6§lSTATISTICS & STORAGE");

                int maxContentWidth = Math.max(leftColWidth, rightColWidth);
                return maxContentWidth + baseMargin;
            }
            case ROSTER, GLOBAL, SETTINGS -> { return 340; }
            default -> { return 250; }
        }
    }
}