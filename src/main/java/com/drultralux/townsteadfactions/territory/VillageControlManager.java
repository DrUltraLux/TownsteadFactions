package com.drultralux.townsteadfactions.territory;

import com.drultralux.townsteadfactions.territory.VillageControlSavedData.VillageControlState;

import java.util.List;
import java.util.Map;

/**
 * The sole gateway for village control state. Mirrors
 * {@code FactionManager}'s ownership pattern.
 */
public final class VillageControlManager {

    /** The currently bound world save data instance, or {@code null} if none is bound. */
    private static VillageControlSavedData activeStorageInstance = null;

    private VillageControlManager() {}

    /**
     * Binds the world save data instance backing this manager's state.
     *
     * @param storage the world save data instance to bind
     */
    public static void setStorageInstance(VillageControlSavedData storage) {
        activeStorageInstance = storage;
    }

    /**
     * Returns the faction currently controlling a village.
     *
     * @param villageKey the village's composite key
     * @return the controlling faction's ID, or {@code null} if uncontrolled
     */
    public static String getControllingFaction(String villageKey) {
        if (activeStorageInstance == null) return null;
        VillageControlState state = activeStorageInstance.villageStates.get(villageKey);
        return (state != null) ? state.controllingFactionId : null;
    }

    /**
     * Checks whether a village is currently contested (tied between two
     * or more factions).
     *
     * @param villageKey the village's composite key
     * @return {@code true} if the village is contested
     */
    public static boolean isContested(String villageKey) {
        if (activeStorageInstance == null) return false;
        VillageControlState state = activeStorageInstance.villageStates.get(villageKey);
        return state != null && state.contested;
    }

    /**
     * Returns the factions currently tied for control of a village, if it's contested.
     *
     * @param villageKey the village's composite key
     * @return the tied factions, or an empty list if not contested
     */
    public static List<String> getContestedFactions(String villageKey) {
        if (activeStorageInstance == null) return List.of();
        VillageControlState state = activeStorageInstance.villageStates.get(villageKey);
        return (state != null) ? state.contestedFactions : List.of();
    }

    /**
     * Updates a village's control state from a fresh population tally.
     * The faction with the highest population takes control. On a tie
     * for the highest population, the village is marked contested and
     * its previous controlling faction is left unchanged, rather than
     * flip-flopping on an exact tie.
     *
     * @param villageKey the village's composite key
     * @param populationByFaction each faction's current population in this village
     */
    public static void updateVillageControl(String villageKey, Map<String, Integer> populationByFaction) {
        if (activeStorageInstance == null || populationByFaction.isEmpty()) return;

        int highestCount = -1;
        for (int count : populationByFaction.values()) {
            if (count > highestCount) highestCount = count;
        }

        final int finalHighestCount = highestCount;
        List<String> leaders = populationByFaction.entrySet().stream()
                .filter(entry -> entry.getValue() == finalHighestCount)
                .map(Map.Entry::getKey)
                .toList();

        VillageControlState state = activeStorageInstance.villageStates.computeIfAbsent(villageKey, k -> new VillageControlState());

        if (leaders.size() > 1) {
            state.contested = true;
            state.contestedFactions.clear();
            state.contestedFactions.addAll(leaders);
            // controllingFactionId deliberately left unchanged on a tie
        } else {
            state.contested = false;
            state.contestedFactions.clear();
            state.controllingFactionId = leaders.get(0);
        }

        activeStorageInstance.setDirty();
    }

    /**
     * Counts how many villages a faction currently controls. Contested
     * villages don't count toward either side, since control is actively
     * in dispute.
     *
     * @param factionId the faction to count
     * @return the number of villages this faction cleanly controls
     */
    public static int getControlledVillageCount(String factionId) {
        if (activeStorageInstance == null || factionId == null) return 0;
        int count = 0;
        for (VillageControlState state : activeStorageInstance.villageStates.values()) {
            if (!state.contested && factionId.equals(state.controllingFactionId)) count++;
        }
        return count;
    }
}
