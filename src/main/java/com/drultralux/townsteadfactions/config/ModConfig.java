package com.drultralux.townsteadfactions.config;

import com.drultralux.townsteadfactions.LogManager;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;

/**
 * Handles initialization, parsing, and registration routing for the split configuration files.
 * Manages client layout caches, common debug flags, and structural faction settings separately.
 */
public class ModConfig {
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ClientConfig CLIENT;

    public static final ModConfigSpec COMMON_SPEC;
    public static final CommonConfig COMMON;

    public static final ModConfigSpec FACTIONS_SPEC;
    public static final FactionsConfig FACTIONS;

    static {
        // Explicit instantiation layout bypassing lambda deprecations
        final ModConfigSpec.Builder clientBuilder = new ModConfigSpec.Builder();
        CLIENT = new ClientConfig(clientBuilder);
        CLIENT_SPEC = clientBuilder.build();

        final ModConfigSpec.Builder commonBuilder = new ModConfigSpec.Builder();
        COMMON = new CommonConfig(commonBuilder);
        COMMON_SPEC = commonBuilder.build();

        final ModConfigSpec.Builder factionsBuilder = new ModConfigSpec.Builder();
        FACTIONS = new FactionsConfig(factionsBuilder);
        FACTIONS_SPEC = factionsBuilder.build();
    }

    /**
     * Contains configuration nodes specifically designated for user-interface positioning parameters.
     * Defined as a static nested class to allow clean instantiation from static initialization blocks.
     */
    public static class ClientConfig {
        public final ModConfigSpec.BooleanValue allowWindowDragging;
        public final ModConfigSpec.IntValue treasuryWidgetX;
        public final ModConfigSpec.IntValue treasuryWidgetY;
        public final ModConfigSpec.IntValue treasuryWidgetTab;

        ClientConfig(ModConfigSpec.Builder builder) {
            builder.push("interface_settings");

            allowWindowDragging = builder
                    .comment("Allows users to drag UI elements and panels between custom tab windows using mouse clicks.")
                    .define("allowWindowDragging", true);

            treasuryWidgetX = builder
                    .comment("The horizontal X position coordinate for the Treasury window widget panel.")
                    .defineInRange("treasuryWidgetX", 100, -2000, 4000);

            treasuryWidgetY = builder
                    .comment("The vertical Y position coordinate for the Treasury window widget panel.")
                    .defineInRange("treasuryWidgetY", 80, -2000, 4000);

            treasuryWidgetTab = builder
                    .comment("The index of the tab page where the Treasury window is currently located (0=Overview, 1=Roster, 2=Global).")
                    .defineInRange("treasuryWidgetTab", 0, 0, 10);

            builder.pop();
        }
    }

    /**
     * Contains configuration nodes designated for general technical parameters and runtime logging switches.
     */
    public static class CommonConfig {
        public final ModConfigSpec.BooleanValue enableDebugLogging;

        CommonConfig(ModConfigSpec.Builder builder) {
            builder.push("technical_settings");
            enableDebugLogging = builder
                    .comment("Outputs explicit tracking messages to console output detailing internal function processing loops.")
                    .define("enableDebugLogging", false);
            builder.pop();
        }
    }

    /**
     * Contains configuration nodes designated for mapping factions, starting balances, and title hierarchies.
     */
    public static class FactionsConfig {
        public final ModConfigSpec.ConfigValue<List<? extends String>> factionRegistryList;

        FactionsConfig(ModConfigSpec.Builder builder) {
            builder.push("faction_definitions");

            // Standard NeoForge clean definition bypassing defineListAllowEmpty warnings completely
            factionRegistryList = builder
                    .comment("List of active faction registration identifiers defined in string layout format.")
                    .defineList("registeredFactions",
                            List.of("solar_vanguard", "lunar_conclave"),
                            obj -> obj instanceof String);

            builder.pop();
        }
    }

    /**
     * Invoked when config loading triggers, updating downstream systems like LogManager directly.
     */
    public static void onConfigLoad() {
        LogManager.setDebugEnabled(COMMON.enableDebugLogging.get());
        LogManager.debug("Configurations processed. Active factions listed: " + FACTIONS.factionRegistryList.get().size());
    }
}