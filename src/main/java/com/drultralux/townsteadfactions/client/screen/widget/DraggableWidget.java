package com.drultralux.townsteadfactions.client.screen.widget;

import dev.marie.MariesLib.client.GuiValueRenderer;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Base abstract window component managing coordinates, drag handlers, and minimize gates.
 */
public abstract class DraggableWidget {
    /** The horizontal screen space layout coordinate position. */
    protected int x;
    /** The vertical screen space layout coordinate position. */
    protected int y;
    /** The pixel width boundary constraints size of this component window container. */
    protected int width;
    /** The pixel height boundary constraints size of this component window container. */
    protected int height;
    /** Toggle state tracking whether this element is actively captured by mouse drag vectors. */
    protected boolean isDragging = false;
    /** Toggle state tracking whether this element is collapsed down to its title bar. */
    protected boolean isMinimized = false;
    /** Relative horizontal capture offset distance calculation metrics. */
    protected int dragOffsetX;
    /** Relative vertical capture offset distance calculation metrics. */
    protected int dragOffsetY;

    /**
     * Allocates standard dimensional parameters to initialize a viewport window frame.
     *
     * @param x initial horizontal viewport pixel vector anchor
     * @param y initial vertical viewport pixel vector anchor
     * @param width structural boundary width parameter size
     * @param height structural boundary height parameter size
     */
    public DraggableWidget(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Executes localized background fill maps and element text prints onto canvas layers.
     *
     * @param graphics the current master pipeline canvas proxy engine
     * @param mouseX active horizontal cursor coordinate intersection point
     * @param mouseY active vertical cursor coordinate intersection point
     * @param partialTicks tick synchronization frame delay intervals ratio
     * @param barRenderer the shared functional MariesLib segment loops utility interface
     */
    public abstract void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, GuiValueRenderer barRenderer);

    /**
     * Evaluates active height vectors to determine cross cursor point overlaps.
     *
     * @param mouseX tracking cursor horizontal position parameter
     * @param mouseY tracking cursor vertical position parameter
     * @return true if mouse intersects the focus collision bounds box zone
     */
    public boolean isHovered(int mouseX, int mouseY) {
        int currentHeight = this.isMinimized ? 14 : this.height;
        return mouseX >= this.x && mouseX < this.x + this.width &&
                mouseY >= this.y && mouseY < this.y + currentHeight;
    }

    /**
     * Tracks collision overlaps specifically targetting the right-side minimize toggle square box.
     *
     * @param mouseX horizontal coordinate metric to cross-examine
     * @param mouseY vertical coordinate metric to cross-examine
     * @return true if cursor sits inside button dimensions
     */
    public boolean isMinimizeButtonHovered(int mouseX, int mouseY) {
        int btnX = this.x + this.width - 14;
        int btnY = this.y + 2;
        return mouseX >= btnX && mouseX < btnX + 10 && mouseY >= btnY && mouseY < btnY + 10;
    }

    /**
     * Monitors click releases to activate localized state triggers or initial drag vectors.
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
     * Processes relative drag increments to actively change layout variables.
     */
    public void mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isDragging) {
            this.x = (int) mouseX - this.dragOffsetX;
            this.y = (int) mouseY - this.dragOffsetY;
        }
    }

    /**
     * Detaches operational click locks when the input trigger clears out.
     */
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isDragging = false;
        }
    }

    /**
     * Fetches the current horizontal layout position vector coordinate.
     *
     * @return the horizontal coordinate integer value
     */
    public int getX() { return this.x; }

    /**
     * Fetches the current vertical layout position vector coordinate.
     *
     * @return the vertical coordinate integer value
     */
    public int getY() { return this.y; }

    /**
     * Manually overrides current screen map placements to hard-set fresh vector positions.
     *
     * @param x explicit target horizontal screen vector position
     * @param y explicit target vertical screen vector position
     */
    public void setPosition(int x, int y) { this.x = x; this.y = y; }
}