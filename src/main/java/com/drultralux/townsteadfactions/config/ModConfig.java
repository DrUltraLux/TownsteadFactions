package com.drultralux.townsteadfactions.config;

import com.drultralux.townsteadfactions.LogManager;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An entirely agnostic configuration processing framework for the modification.
 * Centrally coordinates TOML files for Client/Common via reflection, while handling
 * the Factions setup through a dedicated, open-ended custom JSON backend.
 */
public class ModConfig {
    public static final ModConfigSpec CLIENT_SPEC;
    public static final GenericConfigContainer CLIENT;

    public static final ModConfigSpec COMMON_SPEC;
    public static final GenericConfigContainer COMMON;

    public static final FactionsConfigContainer FACTIONS = new FactionsConfigContainer();

    static {
        final ModConfigSpec.Builder clientBuilder = new ModConfigSpec.Builder();
        CLIENT = new GenericConfigContainer(clientBuilder, new ClientConfigModel(), "interface_settings");
        CLIENT_SPEC = clientBuilder.build();

        final ModConfigSpec.Builder commonBuilder = new ModConfigSpec.Builder();
        COMMON = new GenericConfigContainer(commonBuilder, new CommonConfigModel(), "technical_settings");
        COMMON_SPEC = commonBuilder.build();
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
        } else {
            ModConfigSpec.ConfigValue<?> configValueNode;

            if (payload instanceof Boolean booleanValue) {
                configValueNode = builder.define(nodeKey, booleanValue);
            } else if (payload instanceof Integer integerValue) {
                configValueNode = builder.defineInRange(nodeKey, integerValue, -10000, 10000);
            } else if (payload instanceof List<?> listValue) {
                configValueNode = builder.define(nodeKey, listValue);
            } else {
                configValueNode = builder.define(nodeKey, payload.toString());
            }

            registry.put(nodeKey, configValueNode);
        }
    }

    /**
     * Universal agnostic configuration shell container tracking parsed configuration nodes for any model file.
     */
    public static class GenericConfigContainer {
        /** Complete underlying registry mapping short string keys to active NeoForge runtime fields. */
        public final Map<String, ModConfigSpec.ConfigValue<?>> valuesRegistry = new HashMap<>();
        /** Backing reference pointer to the original instanced configuration model data source class. */
        public final Object backingModelSource;

        GenericConfigContainer(ModConfigSpec.Builder builder, Object modelInstance, String rootGroup) {
            this.backingModelSource = modelInstance;

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
    }

    /**
     * Dedicated dynamic JSON container for faction profiles.
     * Bypasses TOML constraints completely to let players freely add, remove, and rename configuration key sections natively.
     */
    public static class FactionsConfigContainer {
        private final Map<String, List<String>> factionsCache = new HashMap<>();
        public final FactionConfigModel defaultModelData = new FactionConfigModel();

        /**
         * Master dynamic extractor loop that crawls through all loaded list properties.
         * Merges all active custom user configurations without hardcoded keys.
         *
         * @return a completely type-safe collection of all origins loaded from the file system
         */
        public List<String> getFactionRegistryList() {
            List<String> combinedOrigins = new ArrayList<>();
            for (List<String> originsList : this.factionsCache.values()) {
                if (originsList != null) {
                    combinedOrigins.addAll(originsList);
                }
            }
            return combinedOrigins;
        }

        /**
         * Direct getter allowing other manager classes to query the live loaded factions data map.
         */
        public Map<String, List<String>> getFactionsMap() {
            return this.factionsCache;
        }

        /** Internal setter used by the configuration loader to refresh memory layers upon file reads. */
        protected void updateCache(Map<String, List<String>> freshMap) {
            this.factionsCache.clear();
            this.factionsCache.putAll(freshMap);
        }
    }

    /**
     * Invoked safely by ModConfigEvent once client/common TOML sheets are completely memory-bound.
     * Safely updates logging flags and reads your factions JSON without inducing dangerous infinite file-saving loops.
     */
    public static void onConfigLoad() {
        try {
            if (COMMON_SPEC.isLoaded()) {
                LogManager.setDebugEnabled(COMMON.getBoolean("enableDebugLogging", false));
            }

            if (CLIENT_SPEC.isLoaded()) {
                File subDir = new File(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().toFile(), "townsteadfactions");
                if (!subDir.exists()) {
                    subDir.mkdirs();
                }

                File jsonFile = new File(subDir, "factions.json");
                com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();

                if (!jsonFile.exists()) {
                    LogManager.info("First launch context captured. Writing custom indented factions.json structure...");
                    try (FileWriter writer = new FileWriter(jsonFile)) {
                        gson.toJson(FACTIONS.defaultModelData.factions, writer);
                    }
                    FACTIONS.updateCache(FACTIONS.defaultModelData.factions);
                } else {
                    try (FileReader reader = new FileReader(jsonFile)) {
                        Type expectedMapSignature = new com.google.gson.reflect.TypeToken<Map<String, List<String>>>(){}.getType();
                        Map<String, List<String>> loadedData = gson.fromJson(reader, expectedMapSignature);
                        if (loadedData != null) {
                            FACTIONS.updateCache(loadedData);
                        }
                    }
                }

                LogManager.info("All configuration subsystems synchronized. Total active faction origins loaded: " + FACTIONS.getFactionRegistryList().size());
            }
        } catch (Exception e) {
            LogManager.error("Configuration framework failed to update structural file networks!", e);
        }
    }
}