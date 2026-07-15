package com.drultralux.townstead_factions;

import com.aetherianartificer.townstead.root.RootRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static boolean DEBUG_LOG_ENABLED = false;

    // Key = Clean rootID -> Value = Custom Configured Faction Name String
    public static final Map<String, String> ROOT_TO_FACTION_MAP = new HashMap<>();

    // Tracks the absolute list of verified custom faction names defined by Townstead
    public static final Set<String> REGISTERED_FACTIONS = new LinkedHashSet<>();

    public static void loadModpackConfig() {
        try {
            Path configDirectory = Paths.get(System.getProperty("user.dir"), "config");
            File directoryFile = configDirectory.toFile();

            if (!directoryFile.exists()) {
                directoryFile.mkdirs();
            }

            File targetConfigFile = new File(directoryFile, "townstead_factions_rules.json");
            FactionConfigModel activeData;

            if (!targetConfigFile.exists()) {
                LogManager.info("Generating fresh dynamic configuration layout file at: {}", targetConfigFile.getAbsolutePath());
                activeData = new FactionConfigModel();
                try (FileWriter writer = new FileWriter(targetConfigFile)) {
                    GSON.toJson(activeData, writer);
                }
            } else {
                try (FileReader reader = new FileReader(targetConfigFile)) {
                    activeData = GSON.fromJson(reader, FactionConfigModel.class);
                }
                if (activeData == null) {
                    activeData = new FactionConfigModel();
                }
            }

            DEBUG_LOG_ENABLED = activeData.debugLog;
            ROOT_TO_FACTION_MAP.clear();
            REGISTERED_FACTIONS.clear();

            if (activeData.factions != null) {
                // Iterate through every custom faction block declared inside the JSON document
                for (Map.Entry<String, List<String>> entry : activeData.factions.entrySet()) {
                    String factionName = entry.getKey();
                    List<String> assignedRoots = entry.getValue();

                    // Rule: Factions without any assigned rootIDs or empty lists are completely ignored
                    if (assignedRoots == null || assignedRoots.isEmpty()) {
                        LogManager.debug("Ignoring config faction '{}' because its rootID array list is empty.", factionName);
                        continue;
                    }

                    boolean hasAtLeastOneValidRoot = false;

                    for (String rootID : assignedRoots) {
                        if (rootID == null) continue;

                        // Isolate path keyword if developers pass namespaced configurations
                        String cleanID = rootID.contains(":") ? rootID.split(":")[1] : rootID;

                        // Rule: Only map IF the ID matches a real root record currently processed by Townstead
                        boolean isLoadedInTownstead = RootRegistry.all().stream()
                                .anyMatch(root -> root != null && root.id() != null && root.id().getPath().equals(cleanID));

                        if (isLoadedInTownstead) {
                            ROOT_TO_FACTION_MAP.put(cleanID, factionName);
                            hasAtLeastOneValidRoot = true;
                            LogManager.debug("Validated root '{}' -> Assigned to custom faction: {}", cleanID, factionName);
                        } else {
                            // Silently ignore if it doesn't match a loaded rootID
                            LogManager.debug("Silently ignoring unmatched config root ID descriptor: {}", rootID);
                        }
                    }

                    // If the faction successfully processed at least one valid root, register it as active!
                    if (hasAtLeastOneValidRoot) {
                        REGISTERED_FACTIONS.add(factionName);
                    }
                }
            }

            LogManager.info("Dynamic configuration parsing complete! Loaded {} active custom factions.", REGISTERED_FACTIONS.size());

        } catch (Exception e) {
            LogManager.error("Failed to compile user configuration files natively: ", e);
        }
    }
}