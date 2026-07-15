package com.drultralux.townstead_factions.client.screen;

import com.drultralux.townstead_factions.client.ClientFactionCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class FactionScreen extends Screen {

    private enum Tab { OVERVIEW, ROSTER, GLOBAL, SETTINGS }
    private Tab activeTab = Tab.OVERVIEW;
    private static int overviewScrollAmount = 0;
    private boolean isOverviewScrollbarHovered = false;

    private static int frameX = -1;
    private static int frameY = -50;
    private static int frameW = 320;
    private static int frameH = 220;

    private boolean isDraggingFrame = false;
    private boolean isResizingFrame = false;
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;

    private static final int MIN_WIDTH = 250;
    private static final int MIN_HEIGHT = 150;
    private static final int TITLE_BAR_HEIGHT = 16;
    private static final int RESIZE_ZONE_SIZE = 10;

    public FactionScreen() {
        super(Component.literal("Faction Menu"));
    }

    @Override
    protected void init() {
        super.init();

        PacketDistributor.sendToServer(new com.drultralux.townstead_factions.client.MenuRequestPayload());

        // FIXED: Only centers the panel if it has never been initialized or resized by the player yet
        if (frameX == -1) {
            this.frameX = (this.width - this.frameW) / 2;
            this.frameY = (this.height - this.frameH) / 2;
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics, mouseX, mouseY, partialTicks);

        // 1. ALWAYS DRAW THE SOLID BACKGROUND SHAPES AT 1:1 SCALE FIRST!
        // Main dark stone window frame background
        graphics.fill(frameX, frameY, frameX + frameW, frameY + frameH, 0xFF0D0D0D);
        graphics.fill(frameX + 2, frameY + 2, frameX + frameW - 2, frameY + frameH - 2, 0xFF1C1C1C);

        // Top title bar grab panel
        graphics.fill(frameX + 2, frameY + 2, frameX + frameW - 2, frameY + TITLE_BAR_HEIGHT, 0xFF2A2A2A);
        graphics.fill(frameX + 2, frameY + TITLE_BAR_HEIGHT, frameX + frameW - 2, frameY + TITLE_BAR_HEIGHT + 1, 0xFF0D0D0D);

        // Bottom right resizing triangle indicator
        graphics.fill(frameX + frameW - 6, frameY + frameH - 3, frameX + frameW - 3, frameY + frameH - 6, 0xFF505050);
        graphics.fill(frameX + frameW - 4, frameY + frameH - 3, frameX + frameW - 3, frameY + frameH - 4, 0xFF8A8A8A);

        // Render your top selection tabs at 1:1 scale
        renderTopTabs(graphics, mouseX, mouseY);

        // FIXED: Renders the 3D player container box and banner layout at 1:1 scale so it never detaches!
        if (activeTab == Tab.OVERVIEW) {
            renderOverview3DElements(graphics, mouseX, mouseY);
        }

        // 2. NOW APPLY THE MATRIX SCALE SPECIFICALLY FOR THE INNER CONTENT SEGMENTS
        float textScale = ((float) frameW / 320.0F) * 0.85F;
        if (textScale < 0.7F) textScale = 0.7F;
        if (textScale > 1.5F) textScale = 1.5F;

        int contentX = frameX + 15;
        int contentY = frameY + TITLE_BAR_HEIGHT + 20;
        int contentW = frameW - 30;
        int contentH = frameH - TITLE_BAR_HEIGHT - 35;

        // Clip all text lines rendering outside the main border framework container box
        graphics.enableScissor(contentX, contentY, contentX + contentW, contentY + contentH);

        graphics.pose().pushPose();
        // Shift coordinate origin vertically based on the active mouse scroll track position
        graphics.pose().translate(contentX, contentY - overviewScrollAmount, 0.0F);
        graphics.pose().scale(textScale, textScale, 1.0F);

        switch (activeTab) {
            case OVERVIEW -> renderOverviewTab(graphics, 0, 0, mouseX, mouseY);
            case ROSTER -> renderRosterTab(graphics, 0, 0);
            case GLOBAL -> renderGlobalFactionsTab(graphics, 0, 0);
            case SETTINGS -> renderSettingsTab(graphics, 0, 0);
        }

        graphics.pose().popPose();
        graphics.disableScissor(); // Release active mask lock loops safely

        if (activeTab == Tab.OVERVIEW) {
            int totalNeededH = 210;
            if (totalNeededH > contentH) {
                int scrollTrackX = frameX + frameW - 10;
                int barH = Math.max(15, (contentH * contentH) / totalNeededH);
                int maxScroll = totalNeededH - contentH;
                int barY = contentY + ((overviewScrollAmount * (contentH - barH)) / maxScroll);

                this.isOverviewScrollbarHovered = mouseX >= scrollTrackX && mouseX <= scrollTrackX + 6 && mouseY >= contentY && mouseY <= contentY + contentH;

                graphics.fill(scrollTrackX, contentY, scrollTrackX + 6, contentY + contentH, 0xFF0A0A0A);
                graphics.fill(scrollTrackX + 1, barY, scrollTrackX + 5, barY + barH, isOverviewScrollbarHovered ? 0xFF8A8A8A : 0xFF505050);
            }
        }

        super.render(graphics, mouseX, mouseY, partialTicks);

        if (activeTab == Tab.OVERVIEW && this.minecraft != null && this.minecraft.player != null) {
            int playerBoxX = frameX + 15;
            int playerBoxY = frameY + TITLE_BAR_HEIGHT + 20;
            int boxW = 55;
            int boxH = 75;

            Quaternionf entityRotation = new Quaternionf().rotationX((float) Math.PI);
            Quaternionf cameraOrientation = new Quaternionf().rotationXYZ(0.0F, 0.0F, 0.0F);

            InventoryScreen.renderEntityInInventory(
                    graphics,
                    (float) (playerBoxX + (boxW / 2)),              // X center alignment
                    (float) (playerBoxY + boxH - 24),               // FIXED: Raised from -8 to -24 to sit the player's boots cleanly on the inner frame floor!
                    30,                                             // Scale size factor
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    entityRotation,
                    cameraOrientation,
                    (LivingEntity) this.minecraft.player
            );
        }
    }

    private void renderTopTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        String[] tabLabels = {"Overview", "Roster", "Global", "Settings"};
        int count = tabLabels.length;

        // Dynamically scales the widths of your navigation buttons to match whatever size you stretch the screen frame to!
        int availableWidth = frameW - 12;
        int tabW = (availableWidth / count) - 2;
        int tabH = 14;
        int startX = frameX + 6;
        int startY = frameY + TITLE_BAR_HEIGHT + 4;

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
        String currentFaction = ClientFactionCache.getCurrentFaction();
        String cleanRootName = ClientFactionCache.getCurrentCleanOrigin();

        // 1. CALCULATE BOUNDS AND PREVENT COMPONENT CLASHES (Relative to static window size)
        int usableW = frameW - 30;
        int leftBoxW = 110;
        int rightBoxW = usableW - leftBoxW - 10; // Auto-scales width dynamically so columns never touch!

        int topRowH = 82;
        int leftBoxX = x + 65;
        int rightBoxX = leftBoxX + leftBoxW + 10;

        // Panel Box backgrounds (Profiles & Resources)
        graphics.fill(leftBoxX, y, leftBoxX + leftBoxW, y + topRowH, 0xFF0D0D0D);
        graphics.fill(rightBoxX, y, rightBoxX + rightBoxW, y + topRowH, 0xFF0D0D0D);

        // --- SUB-PANEL A: LEFT FACTION STATUS & MEMBERSHIP ---
        int padX = leftBoxX + 6;
        graphics.drawString(this.font, "§6§lMEMBERS", padX, y + 6, 0xFFFFFF);
        graphics.drawString(this.font, "§7Rank: §eLeader", padX, y + 18, 0xFFFFFF);
        graphics.drawString(this.font, "§7Count: §71 / 12", padX, y + 30, 0xFFFFFF);

        // Render 1 filled block out of 12 total to track membership visually!
        drawProgressGridBlocks(graphics, padX, y + 42, 1, 12, 0xFFe29c34);

        graphics.drawString(this.font, "§d§lPOWER", padX, y + 56, 0xFFFFFF);
        // Render 9 filled blocks out of 10 to track 92/100 faction power lines
        drawProgressGridBlocks(graphics, padX, y + 68, 9, 10, 0xFFb55ec5);


        // --- SUB-PANEL B: RIGHT ECONOMIC STATISTICS ---
        int resPadX = rightBoxX + 6;
        graphics.drawString(this.font, "§6§lSTATISTICS & STORAGE", resPadX, y + 6, 0xFFFFFF);

        // Protect text lines from clipping right-side banner elements by imposing sub-offsets
        graphics.drawString(this.font, "§e• Gold Treasury:", resPadX, y + 18, 0xFFFFFF);
        drawProgressGridBlocks(graphics, resPadX, y + 28, 6, 10, 0xFFe0c060); // Mock 60% gold reserve filling

        graphics.drawString(this.font, "§f• Stored Iron Ore:", resPadX, y + 42, 0xFFFFFF);
        drawProgressGridBlocks(graphics, resPadX, y + 52, 4, 10, 0xFF909090); // Mock 40% iron storage filling

        graphics.drawString(this.font, "§b• Lapis Lazuli:", resPadX, y + 66, 0xFFFFFF);


        // --- SUB-PANEL C: BOUNDED RECENT ACTIVITY LOG PANEL WITH NATIVE SCROLLBARS ---
        int logY = y + topRowH + 10;
        int logW = frameW - 30;
        int logH = frameH - logY - 45;

        if (logH > 25) {
            graphics.drawString(this.font, "§6⚙ RECENT ACTIVITY LOG", x, logY, 0xFFFFFF);

            int innerBoxY = logY + 12;
            graphics.fill(x, innerBoxY, x + logW, innerBoxY + logH, 0xFF0D0D0D); // Log sub-box framework

            // Static array tracker of system logs
            String[] logLines = {
                    " §7• " + this.minecraft.player.getScoreboardName() + " §aopened the interface menu screen.",
                    " §7• Server data synchronization payload channel verified cleanly.",
                    " §7• Local core faction capability sheet maps initialized.",
                    " §7• Database sector checks returning successful status blocks.",
                    " §7• Active Townstead registry configurations parsed into memory layers."
            };

            int maxVisibleRows = (logH - 8) / 12;
            int totalRows = logLines.length;

            // DRAW NATIVE RENDER SCROLLBAR ACCENTS IF RECORDS EXTEND PAST VISIBLE COORDINATE LIMITS
            if (totalRows > maxVisibleRows) {
                int scrollTrackX = x + logW - 6;
                int scrollBarH = Math.max(10, (maxVisibleRows * logH) / totalRows);

                // Draw trailing scrolling tracks
                graphics.fill(scrollTrackX, innerBoxY + 2, scrollTrackX + 4, innerBoxY + logH - 2, 0xFF050505);
                // Draw the movable inner scroll slider box handles
                graphics.fill(scrollTrackX + 1, innerBoxY + 4, scrollTrackX + 3, innerBoxY + 4 + scrollBarH, 0xFF505050);
            }

            // Print entries safely within bounds limits
            int rowOffset = 6;
            for (int i = 0; i < Math.min(totalRows, maxVisibleRows); i++) {
                graphics.drawString(this.font, logLines[i], x + 4, innerBoxY + rowOffset, 0xAAAAAA);
                rowOffset += 12;
            }
        }
    }

    private void renderOverview3DElements(GuiGraphics graphics, int mouseX, int mouseY) {
        int contentX = frameX + 15;
        int contentY = frameY + TITLE_BAR_HEIGHT + 20;

        int boxW = 55;
        int boxH = 75;

        // FIXED: Shift boxes vertically so the banner sits cleanly right on top of the player!
        int bannerBoxY = contentY;
        int bannerH = 14;
        int playerBoxY = bannerBoxY + bannerH + 4;

        // Draw Dark Slate Display Shadow Windows at 1:1 scale
        graphics.fill(contentX, bannerBoxY, contentX + boxW, bannerBoxY + bannerH, 0xFF0A0A0A);
        graphics.fill(contentX, playerBoxY, contentX + boxW, playerBoxY + boxH, 0xFF0A0A0A);

        // Render the small compact Crimson Banner layout placeholder graphic
        graphics.fill(contentX + 16, bannerBoxY + 3, contentX + boxW - 16, bannerBoxY + bannerH - 3, 0xFF7A2222);
        graphics.fill(contentX + (boxW / 2) - 1, bannerBoxY + 2, contentX + (boxW / 2) + 1, bannerBoxY + bannerH - 2, 0xFF3A3A3A);
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

    // 4. MOUSE INPUT & MANIPULATION VECTOR INTERCEPTORS (THE DRAG & RESIZE ENGINE)
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Catch Left Clicks only

            // Checking Condition 1: Check if the cursor clicked inside the bottom-right resizing angle zone
            if (mouseX >= (frameX + frameW - RESIZE_ZONE_SIZE) && mouseX <= (frameX + frameW) &&
                    mouseY >= (frameY + frameH - RESIZE_ZONE_SIZE) && mouseY <= (frameY + frameH)) {
                this.isResizingFrame = true;
                return true;
            }

            // Checking Condition 2: Check if the cursor clicked inside the top dragging header grab bar
            if (mouseX >= frameX && mouseX <= (frameX + frameW) &&
                    mouseY >= frameY && mouseY <= (frameY + TITLE_BAR_HEIGHT)) {
                this.isDraggingFrame = true;
                this.dragOffsetX = mouseX - frameX;
                this.dragOffsetY = mouseY - frameY;
                return true;
            }

            // Checking Condition 3: Check if the cursor clicked on an upper tab selection button row
            int availableWidth = frameW - 12;
            int tabW = (availableWidth / Tab.values().length) - 2;
            int tabH = 14;
            int startX = frameX + 6;
            int startY = frameY + TITLE_BAR_HEIGHT + 4;

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
            // Execution Loop A: Process frame expansion resizing operations in real-time
            if (this.isResizingFrame) {
                int calculatedW = (int) (mouseX - frameX);
                int calculatedH = (int) (mouseY - frameY);

                // Enforce safety limits so the UI panel can never be squished into invisibility
                this.frameW = Math.max(calculatedW, MIN_WIDTH);
                this.frameH = Math.max(calculatedH, MIN_HEIGHT);
                return true;
            }

            // Execution Loop B: Shift the panel's absolute screen coordinates based on hand movements
            if (this.isDraggingFrame) {
                this.frameX = (int) (mouseX - dragOffsetX);
                this.frameY = (int) (mouseY - dragOffsetY);
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
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
        // Empty block: Safely terminates the fullscreen dark vignette overlay shader pass!
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Retrieve the current physical GLFW key integer bound to your custom F binding configuration
        int targetToggleKey = KeyMappings.OPEN_FACTION_MENU.getKey().getValue();

        if (keyCode == targetToggleKey) {
            // FIXED: Natively closes the screen container on the spot when hitting F!
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.closeContainer();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

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
            int contentH = frameH - TITLE_BAR_HEIGHT - 35;
            int totalContentHeightNeeded = 210;
            int maxScroll = Math.max(0, totalContentHeightNeeded - contentH);

            // Shift the scrolling position trackers by standard scroll units (12 pixels per wheel notch)
            overviewScrollAmount = (int) (overviewScrollAmount - (scrollY * 12));
            if (overviewScrollAmount < 0) overviewScrollAmount = 0;
            if (overviewScrollAmount > maxScroll) overviewScrollAmount = maxScroll;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}