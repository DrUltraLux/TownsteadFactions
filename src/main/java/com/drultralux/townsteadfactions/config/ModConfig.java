package com.drultralux.townsteadfactions.config;

import com.drultralux.townsteadfactions.LogManager;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An entirely agnostic configuration processing framework for the modification.
 * Employs pure runtime Java reflection to dynamically map object models down to NeoForge TOML structures.
 */
public class ModConfig {
    public static final ModConfigSpec CLIENT_SPEC;
    public static final GenericConfigContainer CLIENT;

    public static final ModConfigSpec COMMON_SPEC;
    public static final GenericConfigContainer COMMON;

    public static final ModConfigSpec FACTIONS_SPEC;
    public static final GenericConfigContainer FACTIONS;

    static {
        final ModConfigSpec.Builder clientBuilder = new ModConfigSpec.Builder();
        CLIENT = new GenericConfigContainer(clientBuilder, new ClientConfigModel(), "interface_settings");
        CLIENT_SPEC = clientBuilder.build();

        final ModConfigSpec.Builder commonBuilder = new ModConfigSpec.Builder();
        COMMON = new GenericConfigContainer(commonBuilder, new CommonConfigModel(), "technical_settings");
        COMMON_SPEC = commonBuilder.build();

        final ModConfigSpec.Builder factionsBuilder = new ModConfigSpec.Builder();
        FACTIONS = new GenericConfigContainer(factionsBuilder, new FactionConfigModel(), "");
        FACTIONS_SPEC = factionsBuilder.build();
    }

    /**
     * Recursively parses any object model structure or map registry to generate agnostic configuration spec nodes.
     *
     * @param builder the active NeoForge configuration layout utility
     * @param modelObject the target model object data source being parsed
     * @param targetRegistry the global memory map collecting active configuration nodes
     */
    public static void parseModelReflectively(ModConfigSpec.Builder builder, Object modelObject, Map<String, ModConfigSpec.ConfigValue<?>> targetRegistry) {
        if (modelObject == null) return;

        if (modelObject instanceof Map<?, ?> rawMap) {
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() instanceof String keyString) {
                    processFieldNode(builder, keyString, entry.getValue(), targetRegistry);
                }
            }
            return;
        }

        for (Field field : modelObject.getClass().getFields()) {
            try {
                String fieldName = field.getName();
                Object fieldPayload = field.get(modelObject);
                processFieldNode(builder, fieldName, fieldPayload, targetRegistry);
            } catch (Exception e) {
                LogManager.error("Agnostic reflection scanner failed to evaluate data field property: " + field.getName(), e);
            }
        }
    }

    /**
     * Internal processor checking object data types to pipe nodes cleanly into type-safe NeoForge blocks.
     */
    public static void processFieldNode(ModConfigSpec.Builder builder, String nodeKey, Object payload, Map<String, ModConfigSpec.ConfigValue<?>> registry) {
        if (payload == null) return;

        if (payload instanceof Map<?, ?>) {
            builder.push(nodeKey);
            parseModelReflectively(builder, payload, registry);
            builder.pop();
        } else if (payload instanceof List<?> rawList) {
            List<String> explicitlyTypedList = new java.util.ArrayList<>();
            for (Object element : rawList) {
                if (element instanceof String stringElement) {
                    explicitlyTypedList.add(stringElement);
                }
            }

            ModConfigSpec.ConfigValue<?> configValueNode;
            if (nodeKey.equals("Mages") || nodeKey.equals("Arcanists") || nodeKey.equals("Machinists") || builder.toString().contains("factions")) {
                configValueNode = builder.define(nodeKey, explicitlyTypedList.toArray(new String[0]));
            } else {
                configValueNode = builder.define(nodeKey, explicitlyTypedList);
            }

            registry.put(nodeKey, configValueNode);
        } else {
            ModConfigSpec.ConfigValue<?> configValueNode;

            if (payload instanceof Boolean booleanValue) {
                configValueNode = builder.define(nodeKey, booleanValue);
            } else if (payload instanceof Integer integerValue) {
                configValueNode = builder.defineInRange(nodeKey, integerValue, -10000, 10000);
            } else {
                configValueNode = builder.define(nodeKey, payload.toString());
            }

            registry.put(nodeKey, configValueNode);
        }
    }

    /**
     * Universal agnostic configuration shell container tracking parsed configuration nodes for any model file.
     * Cleared completely of all suppressed warnings by using strict pattern matching and type-safe collectors.
     */
    public static class GenericConfigContainer {
        /** Complete underlying registry mapping short string keys to active NeoForge runtime fields. */
        public final Map<String, ModConfigSpec.ConfigValue<?>> valuesRegistry = new HashMap<>();
        /** Backing reference pointer to the original instanced configuration model data source class. */
        public final Object backingModelSource;

        GenericConfigContainer(ModConfigSpec.Builder builder, Object modelInstance, String rootGroup) {
            this.backingModelSource = modelInstance;

            // Handles both nested sub-categories and open-ended root layout sheets transparently
            if (rootGroup != null && !rootGroup.isEmpty()) {
                builder.push(rootGroup);
                parseModelReflectively(builder, modelInstance, this.valuesRegistry);
                builder.pop();
            } else {
                parseModelReflectively(builder, modelInstance, this.valuesRegistry);
            }
        }

        /**
         * Universal getter enabling other classes to fetch any boolean field safely by key string path.
         */
        public boolean getBoolean(String key, boolean fallback) {
            if (this.valuesRegistry.containsKey(key) && this.valuesRegistry.get(key).get() instanceof Boolean booleanValue) {
                return booleanValue;
            }
            return fallback;
        }

        /**
         * Universal getter enabling other classes to fetch any integer field safely by key string path.
         */
        public int getInteger(String key, int fallback) {
            if (this.valuesRegistry.containsKey(key) && this.valuesRegistry.get(key).get() instanceof Integer integerValue) {
                return integerValue;
            }
            return fallback;
        }

        /**
         * Universal getter enabling other classes to fetch a clean, warning-free list of strings by key path.
         */
        public List<String> getStringList(String key) {
            if (this.valuesRegistry.containsKey(key) && this.valuesRegistry.get(key).get() instanceof List<?> rawList) {
                List<String> typedList = new ArrayList<>();
                for (Object element : rawList) {
                    if (element instanceof String stringElement) {
                        typedList.add(stringElement);
                    }
                }
                return typedList;
            }
            return List.of();
        }

        /**
         * Master dynamic extractor loop that crawls through all loaded list properties inside Factions.
         * Merges all active custom user configurations without hardcoded keys.
         *
         * @return a completely type-safe collection of all origins loaded from the file system
         */
        public List<String> getFactionRegistryList() {
            List<String> combinedOrigins = new ArrayList<>();
            for (ModConfigSpec.ConfigValue<?> valueNode : this.valuesRegistry.values()) {
                if (valueNode.get() instanceof List<?> rawList) {
                    for (Object element : rawList) {
                        if (element instanceof String stringElement) {
                            combinedOrigins.add(stringElement);
                        }
                    }
                }
            }
            return combinedOrigins;
        }
    }

    /**
     * Invoked safely by ModConfigEvent once files are completely memory-bound.
     * Safely updates logging triggers. Bypasses explicit saves to prevent infinite writing feedback loops.
     */
    public static void onConfigLoad() {
        try {
            if (COMMON_SPEC.isLoaded()) {
                LogManager.setDebugEnabled(COMMON.getBoolean("enableDebugLogging", false));
            }

            if (FACTIONS_SPEC.isLoaded() && CLIENT_SPEC.isLoaded()) {
                // FIXED LIFECYCLE: Completely removed manual .save() triggers from here.
                // NeoForge automatically handles writing default files for us on absolute first boot,
                // so clearing this method node stops the endless file-writing loops completely!
                LogManager.info("Agnostic configuration hierarchies synchronized to memory layouts cleanly.");
            }
        } catch (Exception e) {
            LogManager.warn("Configuration lifecycle skipped early reading step safely.");
        }
    }
}