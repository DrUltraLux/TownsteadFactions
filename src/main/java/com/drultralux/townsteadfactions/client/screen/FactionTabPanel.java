package com.drultralux.townsteadfactions.client.screen;

import com.drultralux.townsteadfactions.client.screen.widget.DraggableWidget;
import dev.marie.MariesLib.client.GuiValueRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates an isolated, dynamic tab screen workspace page panel layout.
 * Houses collections of independent draggable sub-windows to automate compartmentalized rendering loops.
 */
public class FactionTabPanel {
    private final Component tabTitle;
    private final List<DraggableWidget> components = new ArrayList<>();

    /**
     * Instantiates an active viewport layout sheet designated as a standalone UI tab page.
     *
     * @param tabTitle the translated name label displaying over the top selection button
     */
    public FactionTabPanel(Component tabTitle) {
        this.tabTitle = tabTitle;
    }

    /**
     * Pushes a freshly allocated child widget frame down into the local tracking arrays.
     *
     * @param widget the target DraggableWidget instance component frame node to insert
     */
    public void addWidget(DraggableWidget widget) {
        if (widget != null && !this.components.contains(widget)) {
            this.components.add(widget);
        }
    }

    /**
     * Safely drops an active widget instance out of the local sheet matrix during drag movements.
     *
     * @param widget the target DraggableWidget component instance to disconnect and pull
     */
    public void removeWidget(DraggableWidget widget) {
        this.components.remove(widget);
    }

    /**
     * Iterates down the structural tracking stacks to trigger background frame ticks.
     * Passes the functional segment drawing tracker directly through the processing chain loops.
     *
     * @param graphics the core baseline matrix graphics drawing buffer reference
     * @param mouseX the horizontal coordinate vector tracking text elements
     * @param mouseY the vertical coordinate vector tracking text elements
     * @param partialTicks the intermediate physics frame factor index parameter
     * @param barRenderer the centralized MariesLib utility handling segment line updates
     */
    public void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, GuiValueRenderer barRenderer) {
        for (DraggableWidget widget : this.components) {
            widget.render(graphics, mouseX, mouseY, partialTicks, barRenderer);
        }
    }

    /**
     * Fetches the translated text component label held by this layout tracking sheet.
     *
     * @return the text title Component interface object pointer reference
     */
    public Component getTabTitle() {
        return this.tabTitle;
    }

    /**
     * Exposes the raw underlying collection tracking data frames inside the panel view.
     *
     * @return the list container tracking all assigned DraggableWidget objects
     */
    public List<DraggableWidget> getComponents() {
        return this.components;
    }
}