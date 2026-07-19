package com.drultralux.townsteadfactions.titles;

import com.drultralux.townsteadfactions.factions.FactionTitle;

import java.util.UUID;

/**
 * The sole gateway for self-assigned cosmetic faction titles. Mirrors
 * {@code FactionManager}'s ownership pattern — other classes only read or
 * write self-assigned titles through this class's methods.
 */
public final class TitlePreferenceManager {

    /** The currently bound world save data instance, or {@code null} if none is bound. */
    private static TitlePreferenceSavedData activeStorageInstance = null;

    private TitlePreferenceManager() {}

    /**
     * Binds the world save data instance backing this manager's state.
     *
     * @param storage the world save data instance to bind
     */
    public static void setStorageInstance(TitlePreferenceSavedData storage) {
        activeStorageInstance = storage;
    }

    /**
     * Returns a player's self-assigned cosmetic title.
     *
     * @param playerUUID the player to look up
     * @return the player's self-assigned title, or {@code null} if they haven't chosen one
     */
    public static FactionTitle getSelfAssignedTitle(UUID playerUUID) {
        if (activeStorageInstance == null || playerUUID == null) return null;
        return activeStorageInstance.selfAssignedTitles.get(playerUUID);
    }

    /**
     * Sets or clears a player's self-assigned cosmetic title.
     *
     * @param playerUUID the player to update
     * @param title the title to assign, or {@code null} to clear it and revert to the computed default
     */
    public static void setSelfAssignedTitle(UUID playerUUID, FactionTitle title) {
        if (activeStorageInstance == null || playerUUID == null) return;
        if (title == null) {
            activeStorageInstance.selfAssignedTitles.remove(playerUUID);
        } else {
            activeStorageInstance.selfAssignedTitles.put(playerUUID, title);
        }
        activeStorageInstance.setDirty();
    }
}
