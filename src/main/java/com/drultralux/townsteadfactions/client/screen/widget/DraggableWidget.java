package com.drultralux.townsteadfactions.client.screen.widget;

import dev.marie.MariesLib.client.GuiValueRenderer;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Defines a self-contained, interactive window component frame capable of relative
 * position changes, focus tracking, and dynamic mouse drag layout adjustments.
 */
public abstract class DraggableWidget {
    /** The absolute horizontal screen coordinate anchor point. */
    protected int x;
    /** The absolute vertical screen coordinate anchor point. */
    protected int y;
    /** The horizontal size boundary dimensions of the window layout box. */
    protected int width;
    /** The vertical size boundary dimensions of the window layout box. */
    protected int height;
    /** Condition state tracker indicating if a drag transaction is in progress. */
    protected boolean isDragging = false;
    /** Condition state tracker indicating if the rendering viewport is collapsed. */
    protected boolean isMinimized = false;
    /** Horizontal vector distance mapping from the pointer click point. */
    protected int dragOffsetX;
    /** Vertical vector distance mapping from the pointer click point. */
    protected int dragOffsetY;

    /**
     * Allocates standard coordinate anchors to establish a draggable component frame.
     *
     * @param x the initial horizontal position vector coordinate
     * @param y the initial vertical position vector coordinate
     * @param width the default width boundary constraints size
     * @param height the default height boundary constraints size
     */
    public DraggableWidget(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Primary abstract visualization entry point managing pixel output layouts.
     *
     * @param graphics the active core rendering matrix context instance
     * @param mouseX the current horizontal cursor coordinate layer tracking
     * @param mouseY the current vertical cursor coordinate layer tracking
     * @param partialTicks the current intermediate tick frame physics interpolation factor
     * @param barRenderer the shared functional interface wrapper utilized to draw progress lines
     */
    public abstract void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, GuiValueRenderer barRenderer);

    /**
     * Validates if cursor coordinates fall directly inside the active element bounds.
     *
     * @param mouseX the horizontal coordinate of the pointer
     * @param mouseY the vertical coordinate of the pointer
     * @return true if collision detection registers successful boundary intersections
     */
    public boolean isHovered(int mouseX, int mouseY) {
        int currentHeight = this.isMinimized ? 14 : this.height;
        return mouseX >= this.x && mouseX < this.x + this.width &&
                mouseY >= this.y && mouseY < this.y + currentHeight;
    }

    /**
     * Validates if cursor coordinates hit the right minimize button rectangle.
     *
     * @param mouseX the horizontal coordinate of the pointer
     * @param mouseY the vertical coordinate of the pointer
     * @return true if mouse coordinates collide directly with the minimize icon frame
     */
    public boolean isMinimizeButtonHovered(int mouseX, int mouseY) {
        int btnX = this.x + this.width - 14;
        int btnY = this.y + 2;
        return mouseX >= btnX && mouseX < btnX + 10 && mouseY >= btnY && mouseY < btnY + 10;
    }

    /**
     * Direct input click intercept node managing click trigger sequences.
     *
     * @param mouseX the target click horizontal position vector
     * @param mouseY the target click vertical position vector
     * @param button the tracking index code of the active mouse key pressed
     * @return true if input focus processing loops consume the click event
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isMinimizeButtonHovered((int) mouseX, (int) mouseY)) {
                this.isMinimized = !this.isMinimized;
                return true;
            }
            if (isHovered((int) mouseX, (int) mouseY)) {
                this.isDragging = true;
                this.dragOffsetX = (int) mouseX - this.x;
                this.dragOffsetY = (int) mouseY - this.y;
                return true;
            }
        }
        return false;
    }

    /**
     * Intercepts cursor vector transformations to update spatial coordinates for widgets.
     *
     * @param mouseX the tracking vector horizontal pointer coordinate
     * @param mouseY the tracking vector vertical pointer coordinate
     * @param button the mouse click integer descriptor code value
     * @param dragX the net relative change value along the horizontal offset coordinate axis
     * @param dragY the net relative change value along the vertical offset coordinate axis
     */
    public void mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isDragging) {
            this.x = (int) mouseX - this.dragOffsetX;
            this.y = (int) mouseY - this.dragOffsetY;
        }
    }

    /**
     * Standard mouse button release handler node resetting structural tracking locks.
     *
     * @param mouseX the final horizontal click coordinate register point
     * @param mouseY the final vertical click coordinate register point
     * @param button the target mouse key integer index released
     */
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isDragging = false;
        }
    }

    /**
     * Pulls the horizontal vector position component for configuration lookups.
     *
     * @return the active horizontal coordinate integer parameter
     */
    public int getX() { return this.x; }

    /**
     * Pulls the vertical vector position component for configuration lookups.
     *
     * @return the active vertical coordinate integer parameter
     */
    public int getY() { return this.y; }

    /**
     * Adjusts the absolute spatial drawing coordinates of the container box template.
     *
     * @param x the new target horizontal vector screen position layer
     * @param y the new target vertical vector screen position layer
     */
    public void setPosition(int x, int y) { this.x = x; this.y = y; }
}