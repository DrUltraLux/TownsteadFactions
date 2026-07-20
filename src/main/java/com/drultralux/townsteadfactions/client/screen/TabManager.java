package com.drultralux.townsteadfactions.client.screen;

import com.drultralux.townsteadfactions.client.screen.widget.DraggableWidget;
import com.drultralux.townsteadfactions.client.screen.widget.TabPanelWidget;
import com.drultralux.townsteadfactions.utils.LogManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The sole gateway for the faction dashboard's tab layout: which tabs
 * exist, in what order, what widgets are assigned to each, and which tab
 * is currently active. No other class should hold or mutate
 * {@link TabPanelWidget} membership directly — everything goes through
 * this class's methods, the same way faction data only goes through
 * {@code FactionManager}.
 */
public final class TabManager {

    /**
     * The current layout schema version. Bump this when shipping a change
     * that invalidates existing saved layouts — clients with an older
     * saved version automatically reset to defaults on next open.
     */
    public static final int CURRENT_LAYOUT_VERSION = 1;

    /** The stable ID of the default "Overview" tab. */
    public static final String DEFAULT_TAB_OVERVIEW = "overview";

    /** The stable ID of the default "Roster" tab. */
    public static final String DEFAULT_TAB_ROSTER = "roster";

    /** The stable ID of the default "Global" tab. */
    public static final String DEFAULT_TAB_GLOBAL = "global";

    /** The stable ID of the conditional "Leadership" tab, only present while the viewer is currently a leader. */
    public static final String DEFAULT_TAB_LEADERSHIP = "leadership";

    /**
     * Checks whether a tab is protected — exempt from being manually
     * closed, and exempt from counting toward the "at least one regular
     * tab must always remain" minimum. Currently only the conditional
     * Leadership tab is protected; any future non-deletable conditional
     * tabs should be added to this check too.
     *
     * @param tabId the tab ID to check
     * @return {@code true} if this tab is protected
     */
    public static boolean isProtectedTab(String tabId) {
        return DEFAULT_TAB_LEADERSHIP.equals(tabId);
    }

    /**
     * Counts how many non-protected (regular, user-manageable) tabs
     * currently exist.
     *
     * @return the regular tab count
     */
    public static long getRegularTabCount() {
        return tabs.stream().filter(t -> !isProtectedTab(t.getId())).count();
    }

    /** The tabs currently making up the dashboard, in display order. */
    private static final List<TabPanelWidget> tabs = new ArrayList<>();

    /** The ID of the currently active tab, or {@code null} if there are no tabs. */
    private static String activeTabId = null;

    /** The fixed width/height of the "add tab" button. */
    private static final int ADD_BUTTON_SIZE = 14;

    /** The "add tab" button's current x position, assigned once per frame by the header layout pass. */
    private static int addButtonX;

    /** The "add tab" button's current y position, assigned once per frame by the header layout pass. */
    private static int addButtonY;

    private TabManager() {}

    /**
     * The outcome of a click against the tab header row.
     *
     * @param consumed whether the click landed on a header/button and
     *                 should not be processed further by the caller
     * @param renameRequestedTabId the ID of a tab whose rename was
     *                              requested via double click, or
     *                              {@code null} if none was requested
     * @param closeRequestedTabId the ID of a tab whose removal was
     *                             requested via its close button, or
     *                             {@code null} if none was requested
     */
    public record HeaderClickOutcome(boolean consumed, String renameRequestedTabId, String closeRequestedTabId) {}

    /**
     * Creates a new, empty tab with a freshly generated stable ID and adds
     * it to the end of the tab list. If this is the first tab created, it
     * becomes the active tab.
     *
     * @param title the new tab's display name
     * @return the new tab's generated ID
     */
    public static String addTab(String title) {
        String id = "tab_" + UUID.randomUUID().toString().substring(0, 8);
        tabs.add(new TabPanelWidget(id, title));
        if (activeTabId == null) {
            activeTabId = id;
        }
        return id;
    }

    /**
     * Removes a tab, moving any widgets it contained onto the first
     * remaining tab. Refuses to remove the last tab, since the dashboard
     * always needs at least one.
     *
     * @param tabId the ID of the tab to remove
     * @return {@code true} if the tab was removed
     */
    public static boolean removeTab(String tabId) {
        TabPanelWidget target = findTab(tabId);
        if (target == null) return false;

        // Only regular tabs count toward the "must keep at least one" minimum — removing a
        // protected tab (e.g. Leadership, when resigning) is never blocked by this check.
        if (!isProtectedTab(tabId) && getRegularTabCount() <= 1) return false;

        // Prefer a regular tab as the destination for the removed tab's widgets, so they never
        // land on a conditional tab that could later disappear on its own (e.g. Leadership).
        TabPanelWidget fallback = null;
        for (TabPanelWidget candidate : tabs) {
            if (candidate != target && !isProtectedTab(candidate.getId())) {
                fallback = candidate;
                break;
            }
        }
        if (fallback == null) {
            for (TabPanelWidget candidate : tabs) {
                if (candidate != target) {
                    fallback = candidate;
                    break;
                }
            }
        }
        if (fallback == null) return false;

        for (DraggableWidget widget : new ArrayList<>(target.getComponents())) {
            target.removeWidget(widget);
            fallback.addWidget(widget);
        }

        tabs.remove(target);
        if (tabId.equals(activeTabId)) {
            activeTabId = fallback.getId();
        }
        return true;
    }

    public static boolean removeTabDiscardingWidgets(String tabId) {
        TabPanelWidget target = findTab(tabId);
        if (target == null) return false;
        if (!isProtectedTab(tabId) && getRegularTabCount() <= 1) return false;

        tabs.remove(target);
        if (tabId.equals(activeTabId)) {
            activeTabId = tabs.isEmpty() ? null : tabs.get(0).getId();
        }
        return true;
    }

    /**
     * Renames a tab.
     *
     * @param tabId the ID of the tab to rename
     * @param newTitle the new title; ignored if blank
     */
    public static void renameTab(String tabId, String newTitle) {
        TabPanelWidget tab = findTab(tabId);
        if (tab != null) {
            tab.setTitle(newTitle);
        }
    }

    /**
     * Moves a tab to a new position in the display order.
     *
     * @param tabId the ID of the tab to move
     * @param newIndex the target index; clamped to the valid range
     */
    public static void reorderTab(String tabId, int newIndex) {
        TabPanelWidget tab = findTab(tabId);
        if (tab == null) return;

        int clampedIndex = Math.max(0, Math.min(newIndex, tabs.size() - 1));
        tabs.remove(tab);
        tabs.add(clampedIndex, tab);
    }

    /**
     * Moves a widget onto a tab, removing it from whichever tab (if any)
     * currently holds it first.
     *
     * @param widget the widget to move
     * @param tabId the ID of the destination tab
     */
    public static void moveWidgetToTab(DraggableWidget widget, String tabId) {
        if (widget == null || tabs.isEmpty()) return;
        TabPanelWidget target = findTab(tabId);
        if (target == null) {
            target = tabs.get(0);
        }

        for (TabPanelWidget tab : tabs) {
            tab.removeWidget(widget);
        }
        target.addWidget(widget);
    }

    /**
     * Returns the live list of tabs, in display order, for rendering and
     * hit-testing. Callers should not add, remove, or reorder entries in
     * the returned list directly — use this class's methods instead.
     *
     * @return the current tabs
     */
    public static List<TabPanelWidget> getTabs() {
        return tabs;
    }

    /**
     * Finds a tab by ID.
     *
     * @param tabId the tab ID to look up
     * @return the matching tab, or {@code null} if none exists
     */
    public static TabPanelWidget findTab(String tabId) {
        for (TabPanelWidget tab : tabs) {
            if (tab.getId().equals(tabId)) {
                return tab;
            }
        }
        return null;
    }

    /**
     * Returns the ID of the currently active tab.
     *
     * @return the active tab's ID, or {@code null} if there are no tabs
     */
    public static String getActiveTabId() {
        return activeTabId;
    }

    /**
     * Returns the currently active tab.
     *
     * @return the active tab, or {@code null} if there are no tabs
     */
    public static TabPanelWidget getActiveTab() {
        return findTab(activeTabId);
    }

    /**
     * Sets the active tab.
     *
     * @param tabId the ID of the tab to activate; ignored if unknown
     */
    public static void setActiveTab(String tabId) {
        if (findTab(tabId) != null) {
            activeTabId = tabId;
        }
    }

    /**
     * Computes and assigns each tab header's on-screen position, left to
     * right starting at the given origin, followed by the "add tab"
     * button. Must be called once per frame, before rendering or
     * hit-testing any tab header or the add button, so all three stay
     * consistent with each other.
     *
     * @param mainX the x position of the dashboard window
     * @param mainY the y position of the dashboard window
     * @param font the font headers will be measured and drawn with
     */
    public static void layoutHeaders(int mainX, int mainY, Font font) {
        int tabX = mainX + 20;
        int tabY = mainY + 8;
        for (TabPanelWidget tab : tabs) {
            int textWidth = font.width(tab.getTitle());
            tab.setHeaderBounds(tabX, tabY, textWidth);
            tabX += tab.getTotalHeaderWidth() + 16;
        }
        addButtonX = tabX;
        addButtonY = tabY;
    }

    /**
     * Renders the "add tab" button at its current layout position.
     *
     * @param graphics the graphics context to draw with
     * @param font the font to draw the button label with
     */
    public static void renderAddButton(GuiGraphics graphics, Font font) {
        graphics.fill(addButtonX, addButtonY - 2, addButtonX + ADD_BUTTON_SIZE, addButtonY + 12, 0xFF222222);
        graphics.renderOutline(addButtonX, addButtonY - 2, ADD_BUTTON_SIZE, 14, 0xFF555555);
        graphics.drawString(font, "+", addButtonX + 4, addButtonY + 1, 0xFFAAAAAA, false);
    }

    /**
     * Checks whether the given point falls within the "add tab" button's
     * bounds.
     *
     * @param mouseX the x position to check
     * @param mouseY the y position to check
     * @return {@code true} if the point is within the add button
     */
    public static boolean isAddButtonHovered(double mouseX, double mouseY) {
        return mouseX >= addButtonX && mouseX <= addButtonX + ADD_BUTTON_SIZE &&
                mouseY >= addButtonY - 2 && mouseY <= addButtonY + 12;
    }

    /**
     * Checks a click against every tab's header bounds (which must already
     * be up to date via {@link #layoutHeaders}), applying tab selection
     * directly and reporting rename requests back to the caller.
     *
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     * @return the outcome of the click
     */
    public static HeaderClickOutcome handleHeaderClick(double mouseX, double mouseY) {
        for (TabPanelWidget tab : tabs) {
            boolean closeable = !isProtectedTab(tab.getId()) && getRegularTabCount() > 1;
            switch (tab.mouseClicked(mouseX, mouseY, closeable)) {
                case SELECTED -> {
                    setActiveTab(tab.getId());
                    return new HeaderClickOutcome(true, null, null);
                }
                case RENAME_REQUESTED -> {
                    return new HeaderClickOutcome(true, tab.getId(), null);
                }
                case CLOSE_REQUESTED -> {
                    return new HeaderClickOutcome(true, null, tab.getId());
                }
                case NONE -> { /* keep checking the remaining tabs */ }
            }
        }
        return new HeaderClickOutcome(false, null, null);
    }

    /**
     * Clears all tabs and installs the three default tabs (Overview,
     * Roster, Global), with Overview active. Does not create or place any
     * widgets — the caller is responsible for placing default widgets
     * using {@link #DEFAULT_TAB_OVERVIEW}, {@link #DEFAULT_TAB_ROSTER},
     * and {@link #DEFAULT_TAB_GLOBAL}.
     */
    public static void installDefaultLayout() {
        tabs.clear();
        tabs.add(new TabPanelWidget(DEFAULT_TAB_OVERVIEW, "Overview"));
        tabs.add(new TabPanelWidget(DEFAULT_TAB_ROSTER, "Roster"));
        tabs.add(new TabPanelWidget(DEFAULT_TAB_GLOBAL, "Global"));
        activeTabId = DEFAULT_TAB_OVERVIEW;
    }

    /**
     * Ensures the Leadership tab exists if it currently should be
     * visible, or removes it if it shouldn't — cheap to call every
     * frame, since it's a no-op once the tab's presence already matches
     * the desired state. Deliberately not part of the default tab set
     * created by {@link #installDefaultLayout} — this tab only exists at
     * all while the viewer is currently a leader.
     *
     * @param shouldBeVisible whether the Leadership tab should currently exist
     * @return {@code true} if the tab was just newly created by this call,
     *         so the caller knows to place its widget onto it for the first time
     */
    public static boolean syncLeadershipTabVisibility(boolean shouldBeVisible) {
        boolean exists = findTab(DEFAULT_TAB_LEADERSHIP) != null;
        if (shouldBeVisible && !exists) {
            tabs.add(new TabPanelWidget(DEFAULT_TAB_LEADERSHIP, "Leadership"));
            return true;
        }
        if (!shouldBeVisible && exists) {
            removeTabDiscardingWidgets(DEFAULT_TAB_LEADERSHIP);
        }
        return false;
    }

    /**
     * Rebuilds the tab list from previously saved entries, each formatted
     * as {@code "id;title"}, preserving each tab's saved ID rather than
     * generating new ones. Used when loading a saved layout from config.
     *
     * @param encodedTabs the saved tab entries
     * @return {@code true} if at least one tab was successfully loaded
     */
    public static boolean loadFromEncoded(List<String> encodedTabs) {
        tabs.clear();
        for (String entry : encodedTabs) {
            if (entry == null || !entry.contains(";")) {
                LogManager.warn("Skipping malformed saved tab entry (expected 'id;title'): '" + entry + "'");
                continue;
            }
            String[] tokens = entry.split(";", 2);
            if (tokens.length < 2 || tokens[0].trim().isEmpty()) {
                LogManager.warn("Skipping malformed saved tab entry (expected 'id;title'): '" + entry + "'");
                continue;
            }

            String tabId = tokens[0].trim();
            if (findTab(tabId) != null) {
                LogManager.warn("Skipping duplicate saved tab entry with ID '" + tabId + "' — a tab with this ID was already loaded.");
                continue;
            }

            tabs.add(new TabPanelWidget(tabId, tokens[1].trim()));
        }
        if (!tabs.isEmpty()) {
            activeTabId = tabs.get(0).getId();
        }
        return !tabs.isEmpty();
    }

    /**
     * Computes the minimum window width, in pixels, that would avoid
     * clipping any widget in any tab, based on each widget's current
     * position relative to the window's left edge. Takes the maximum
     * requirement across all tabs, so the window can never be resized
     * below a width that would clip a widget in a tab you aren't
     * currently viewing — switching tabs afterward never forces an
     * unexpected resize.
     *
     * @param mainX the window's current absolute left-edge x position
     * @return the minimum width, in pixels, required across all tabs
     */
    public static int getGlobalMinimumWidth(int mainX) {
        int globalMin = 200; // absolute floor, enough for tab headers and the add-tab button
        for (TabPanelWidget tab : tabs) {
            for (DraggableWidget widget : tab.getComponents()) {
                int requiredWidth = (widget.getX() - mainX) + widget.getWidth() + 10;
                globalMin = Math.max(globalMin, requiredWidth);
            }
        }
        return globalMin;
    }

    /**
     * Resets the dashboard to its default tab layout, discarding all
     * current tabs and widget placements. Used by the layout-version and
     * admin-triggered reset mechanisms.
     *
     * <p>Note: this only resets tab structure. The caller is responsible
     * for re-placing the dashboard's widget instances onto the resulting
     * default tabs afterward.</p>
     */
    public static void forceResetToDefaults() {
        installDefaultLayout();
    }
}