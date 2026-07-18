package com.drultralux.townsteadfactions.layout;

import java.util.UUID;

/**
 * The sole gateway for faction dashboard layout reset state: the global
 * reset counter and the set of players with a targeted reset pending
 * delivery. Mirrors how {@code FactionManager} owns faction data — other
 * classes should only interact with reset state through this class's
 * methods, never through a {@link LayoutResetSavedData} instance
 * directly.
 */
public final class LayoutResetManager {

    /** The currently bound world save data instance, or {@code null} if none is bound. */
    private static LayoutResetSavedData activeStorageInstance = null;

    private LayoutResetManager() {}

    /**
     * Binds the world save data instance backing this manager's state.
     *
     * @param storage the world save data instance to bind
     */
    public static void setStorageInstance(LayoutResetSavedData storage) {
        activeStorageInstance = storage;
    }

    /**
     * Returns the current global reset version, sent to clients on login
     * so they can detect a reset they haven't applied yet.
     *
     * @return the current global reset version, or {@code 0} if storage isn't bound
     */
    public static int getGlobalResetVersion() {
        return (activeStorageInstance != null) ? activeStorageInstance.globalResetVersion : 0;
    }

    /**
     * Bumps the global reset version, so every client — online now, or
     * logging in later — will detect and apply a layout reset.
     */
    public static void triggerGlobalReset() {
        if (activeStorageInstance == null) return;
        activeStorageInstance.globalResetVersion++;
        activeStorageInstance.setDirty();
    }

    /**
     * Marks a player as having a layout reset queued for their next
     * login. Intended for players who are currently offline; online
     * players should be sent the reset directly instead.
     *
     * @param playerUUID the UUID of the player to queue a reset for
     */
    public static void markPlayerPendingReset(UUID playerUUID) {
        if (activeStorageInstance == null || playerUUID == null) return;
        activeStorageInstance.pendingPlayerResets.add(playerUUID);
        activeStorageInstance.setDirty();
    }

    /**
     * Checks whether a player has a targeted reset pending, and clears the
     * flag if so. Intended to be called once, at login.
     *
     * @param playerUUID the UUID of the logging-in player
     * @return {@code true} if a pending reset was found and cleared
     */
    public static boolean consumePendingReset(UUID playerUUID) {
        if (activeStorageInstance == null || playerUUID == null) return false;
        boolean wasPending = activeStorageInstance.pendingPlayerResets.remove(playerUUID);
        if (wasPending) {
            activeStorageInstance.setDirty();
        }
        return wasPending;
    }
}