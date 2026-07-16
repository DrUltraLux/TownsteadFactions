package com.drultralux.townsteadfactions.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FactionConfigModel {

    // A LinkedHashMap preserves the visual ordering of factions exactly as written in the JSON file
    public Map<String, List<String>> factions = new LinkedHashMap<>();

    public FactionConfigModel() {
        List<String> mages = new ArrayList<>();
        mages.add("townstead_classic:blood_orc");
        mages.add("townstead_classic:wild_orc");
        mages.add("townstead_classic:celestial");
        mages.add("townstead_classic:tiefling");
        mages.add("townstead_classic:halfling");
        factions.put("Mages", mages);

        List<String> arcanists = new ArrayList<>();
        arcanists.add("townstead_classic:high_elf");
        arcanists.add("townstead_classic:dark_elf");
        arcanists.add("townstead_classic:wood_elf");
        factions.put("Arcanists", arcanists);

        List<String> machinists = new ArrayList<>();
        machinists.add("townstead_classic:mountain_dwarf");
        machinists.add("townstead_classic:hill_dwarf");
        machinists.add("townstead_classic:tinker_gnome");
        machinists.add("townstead_classic:deepwood_gnome");
        machinists.add("townstead_classic:goblin");
        factions.put("Machinists", machinists);
    }
}
