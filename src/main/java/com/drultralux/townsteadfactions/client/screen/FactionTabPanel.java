package com.drultralux.townsteadfactions.client.screen;

import com.drultralux.townsteadfactions.client.screen.widget.DraggableWidget;
import dev.marie.MariesLib.client.GuiValueRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Standard container layer grouping separate widget models together onto isolated tab sheet frames.
 */
public class FactionTabPanel {
    /** The text label printed atop the dashboard navigation header strip buttons layer. */
    private final Component tabTitle;
    /** Local collections index mapping child window entities tied to this tab environment. */
    private final List<DraggableWidget> components = new ArrayList<>();

    /**
     * Binds a display name signature to an isolated tab panel sheet object container instance.
     *
     * @param tabTitle translated text component label to pass down
     */
    public FactionTabPanel(Component tabTitle) {
        this.tabTitle = tabTitle;
    }

    /**
     * appends an interactive window item reference layout down into the tracking registry arrays.
     *
     * @param widget the child widget component layout object to add
     */
    public void addWidget(DraggableWidget widget) {
        if (widget != null && !this.components.contains(widget)) {
            this.components.add(widget);
        }
    }

    /**
     * Flushes a target widget trace reference right out of local mapping sheet arrays.
     *
     * @param widget target child element profile context to drop
     */
    public void removeWidget(DraggableWidget widget) {
        this.components.remove(widget);
    }

    /**
     * Loops through tracking tables to execute frame updates across all populated sub-windows.
     *
     * @param graphics master engine drawing canvas context pipeline
     * @param mouseX tracking horizontal cursor pointer pixel position
     * @param mouseY tracking vertical cursor pointer pixel position
     * @param partialTicks tick frame synchronization delay coefficient ratio
     * @param barRenderer the shared functional MariesLib segment loops utility interface passed from screen
     */
    public void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, GuiValueRenderer barRenderer) {
        for (DraggableWidget widget : this.components) {
            widget.render(graphics, mouseX, mouseY, partialTicks, barRenderer);
        }
    }

    /**
     * Returns the base translated text label matching this tab panel instance.
     *
     * @return the localized string text component instance
     */
    public Component getTabTitle() {
        return this.tabTitle;
    }

    /**
     * Returns an unmodifiable reference tracking all stored sub-component widget layout profiles.
     *
     * @return active tracking list containing children
     */
    public List<DraggableWidget> getComponents() {
        return this.components;
    }
}