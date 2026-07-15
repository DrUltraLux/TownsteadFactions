package com.drultralux.townstead_factions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FactionConfigModel {
    public boolean debugLog = false;

    // A LinkedHashMap preserves the visual ordering of factions exactly as written in the JSON file
    public Map<String, List<String>> factions = new LinkedHashMap<>();

    public FactionConfigModel() {
        // Set up the default template matching your original setup on first boot
        List<String> mages = new ArrayList<>();
        mages.add("blood_orc");
        mages.add("wild_orc");
        mages.add("celestial");
        mages.add("tiefling");
        mages.add("halfling");
        factions.put("Mages", mages);

        List<String> arcanists = new ArrayList<>();
        arcanists.add("high_elf");
        arcanists.add("dark_elf");
        arcanists.add("wood_elf");
        factions.put("Arcanists", arcanists);

        List<String> machinists = new ArrayList<>();
        machinists.add("mountain_dwarf");
        machinists.add("hill_dwarf");
        machinists.add("tinker_gnome");
        machinists.add("deepwood_gnome");
        machinists.add("goblin");
        factions.put("Machinists", machinists);
    }
}
