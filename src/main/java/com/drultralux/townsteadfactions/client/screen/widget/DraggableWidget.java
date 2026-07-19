package com.drultralux.townsteadfactions.client.screen.widget;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Base class for a draggable, minimizable widget in the faction dashboard.
 * Handles position, size, dragging, and minimize/restore state; subclasses
 * are responsible for their own rendering.
 */
public abstract class DraggableWidget {

    /** The widget's current x position, in screen pixels. */
    protected int x;

    /** The widget's current y position, in screen pixels. */
    protected int y;

    /** The widget's width, in pixels. */
    protected int width;

    /** The widget's height, in pixels. */
    protected int height;

    /** Whether the widget is currently being dragged. */
    protected boolean isDragging = false;

    /** Whether the widget is currently minimized. */
    protected boolean isMinimized = false;

    /** The x offset between the widget's origin and the mouse when a drag started. */
    protected int dragOffsetX;

    /** The y offset between the widget's origin and the mouse when a drag started. */
    protected int dragOffsetY;

    /**
     * Creates a widget at the given position and size.
     *
     * @param x the initial x position
     * @param y the initial y position
     * @param width the widget's width
     * @param height the widget's height
     */
    public DraggableWidget(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Renders this widget. Implemented by subclasses.
     *
     * @param graphics the graphics context to draw with
     * @param mouseX the current mouse x position
     * @param mouseY the current mouse y position
     * @param partialTicks the partial tick time, for frame interpolation
     */
    public abstract void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks);

    /**
     * Checks whether the given point falls within this widget's bounds,
     * using its minimized height if currently minimized.
     *
     * @param mouseX the x position to check
     * @param mouseY the y position to check
     * @return {@code true} if the point is within the widget's bounds
     */
    public boolean isHovered(int mouseX, int mouseY) {
        int currentHeight = this.isMinimized ? 14 : this.height;
        return mouseX >= this.x && mouseX < this.x + this.width &&
                mouseY >= this.y && mouseY < this.y + currentHeight;
    }

    /**
     * Checks whether the given point falls within the minimize button's
     * bounds, in the widget's top-right corner.
     *
     * @param mouseX the x position to check
     * @param mouseY the y position to check
     * @return {@code true} if the point is within the minimize button
     */
    public boolean isMinimizeButtonHovered(int mouseX, int mouseY) {
        int btnX = this.x + this.width - 14;
        int btnY = this.y + 2;
        return mouseX >= btnX && mouseX < btnX + 10 && mouseY >= btnY && mouseY < btnY + 10;
    }

    /**
     * Handles a mouse click: toggles minimized state if the minimize
     * button was clicked, or begins dragging if the widget body was
     * clicked.
     *
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     * @param button the mouse button pressed
     * @return {@code true} if the click was handled by this widget
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
     * Updates this widget's position to follow the mouse while dragging.
     *
     * @param mouseX the current mouse x position
     * @param mouseY the current mouse y position
     * @param button the mouse button held
     * @param dragX the horizontal drag delta, unused
     * @param dragY the vertical drag delta, unused
     */
    public void mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isDragging) {
            this.x = (int) mouseX - this.dragOffsetX;
            this.y = (int) mouseY - this.dragOffsetY;
        }
    }

    /**
     * Ends a drag when the mouse button is released.
     *
     * @param mouseX the mouse x position, unused
     * @param mouseY the mouse y position, unused
     * @param button the mouse button released
     */
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isDragging = false;
        }
    }

    /**
     * Returns this widget's current x position.
     *
     * @return the x position
     */
    public int getX() { return this.x; }

    /**
     * Returns this widget's current y position.
     *
     * @return the y position
     */
    public int getY() { return this.y; }

    /**
     * Returns this widget's width.
     *
     * @return the width, in pixels
     */
    public int getWidth() { return this.width; }

    /**
     * Returns this widget's height.
     *
     * @return the height, in pixels
     */
    public int getHeight() { return this.height; }

    /**
     * Sets this widget's position directly.
     *
     * @param x the new x position
     * @param y the new y position
     */
    public void setPosition(int x, int y) { this.x = x; this.y = y; }

    /**
     * Handles a mouse-wheel scroll. Default no-op; overridden by widgets
     * that support scrolling (e.g. the activity log).
     *
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     * @param scrollX the horizontal scroll amount, unused by most widgets
     * @param scrollY the vertical scroll amount
     * @return {@code true} if the scroll was handled by this widget
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }
}