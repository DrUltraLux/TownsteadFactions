package com.drultralux.townsteadfactions.client.screen;

import com.drultralux.townsteadfactions.client.screen.widget.DraggableWidget;
import dev.marie.MariesLib.client.GuiValueRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single tab in the faction dashboard, holding the set of
 * draggable widgets assigned to that tab.
 */
public class FactionTabPanel {

    /** The display name shown on this tab's header button. */
    private final Component tabTitle;

    /** The widgets currently assigned to this tab. */
    private final List<DraggableWidget> components = new ArrayList<>();

    /**
     * Creates a new, empty tab panel.
     *
     * @param tabTitle the display name shown on this tab's header button
     */
    public FactionTabPanel(Component tabTitle) {
        this.tabTitle = tabTitle;
    }

    /**
     * Adds a widget to this tab, if it isn't already present.
     *
     * @param widget the widget to add
     */
    public void addWidget(DraggableWidget widget) {
        if (widget != null && !this.components.contains(widget)) {
            this.components.add(widget);
        }
    }

    /**
     * Removes a widget from this tab.
     *
     * @param widget the widget to remove
     */
    public void removeWidget(DraggableWidget widget) {
        this.components.remove(widget);
    }

    /**
     * Renders every widget assigned to this tab.
     *
     * @param graphics the graphics context to draw with
     * @param mouseX the current mouse x position
     * @param mouseY the current mouse y position
     * @param partialTicks the partial tick time, for frame interpolation
     * @param barRenderer a shared segment renderer for resource bars.
     *                     TODO: this is currently unused — widgets aren't given
     *                     access to it. Confirm whether it's meant to be wired
     *                     into widget rendering, or removed if not needed.
     */
    public void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, GuiValueRenderer barRenderer) {
        for (DraggableWidget widget : this.components) {
            widget.render(graphics, mouseX, mouseY, partialTicks);
        }
    }

    /**
     * Returns this tab's display name.
     *
     * @return the tab title
     */
    public Component getTabTitle() {
        return this.tabTitle;
    }

    /**
     * Returns the live list of widgets assigned to this tab. Modifying the
     * returned list modifies this panel directly.
     *
     * @return the widgets assigned to this tab
     */
    public List<DraggableWidget> getComponents() {
        return this.components;
    }
}