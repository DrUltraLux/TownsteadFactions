package com.drultralux.townsteadfactions.config;

import com.drultralux.townsteadfactions.factions.FactionManager;
import com.drultralux.townsteadfactions.utils.LogManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central configuration handler for Townstead Factions. Builds the
 * client/common TOML config specs reflectively from plain model objects,
 * and separately manages the faction-to-origin mapping as a hand-editable
 * JSON file.
 */
public class ModConfig {

    /** The built NeoForge config spec for client-side settings. */
    public static final ModConfigSpec CLIENT_SPEC;

    /** The client-side settings container, backed by {@link ClientConfigModel}. */
    public static final GenericConfigContainer CLIENT;

    /** The built NeoForge config spec for common (client+server) settings. */
    public static final ModConfigSpec COMMON_SPEC;

    /** The common settings container, backed by {@link CommonConfigModel}. */
    public static final GenericConfigContainer COMMON;

    /** The faction-to-origin mapping, loaded from {@code factions.json}. */
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
     * Recursively walks a model object's public fields (or a map's
     * entries) and registers a corresponding config value for each one.
     *
     * @param builder the config spec builder to register values on
     * @param modelObject the model instance or map to parse
     * @param targetRegistry the map to record each registered config value into, keyed by field/entry name
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
     * Registers a single config value based on the runtime type of
     * {@code payload}: nested maps become a config group, and booleans,
     * integers, and lists get typed config values; anything else falls
     * back to a string value via {@link Object#toString()}.
     *
     * @param builder the config spec builder to register the value on
     * @param nodeKey the config key for this value
     * @param payload the value to register
     * @param registry the map to record the registered config value into
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
     * A config container built reflectively from a plain model object,
     * providing typed getters for its values by key.
     */
    public static class GenericConfigContainer {

        /** Registered config values, keyed by field/entry name. */
        public final Map<String, ModConfigSpec.ConfigValue<?>> valuesRegistry = new HashMap<>();

        /** The model instance this container was built from. */
        public final Object backingModelSource;

        /**
         * Builds a config container from a model instance, optionally
         * nested under a root config group.
         *
         * @param builder the config spec builder to register values on
         * @param modelInstance the model object to parse
         * @param rootGroup the config group name to nest values under, or {@code null}/empty for none
         */
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
         * Reads a string config value by key.
         *
         * @param key the config key to read
         * @param fallback the value to return if the key is missing or not a string
         * @return the config value, or {@code fallback}
         */
        public String getString(String key, String fallback) {
            if (this.valuesRegistry.containsKey(key) && this.valuesRegistry.get(key).get() instanceof String stringValue) {
                return stringValue;
            }
            return fallback;
        }

        /**
         * Reads a boolean config value by key.
         *
         * @param key the config key to read
         * @param fallback the value to return if the key is missing or not a boolean
         * @return the config value, or {@code fallback}
         */
        public boolean getBoolean(String key, boolean fallback) {
            if (this.valuesRegistry.containsKey(key) && this.valuesRegistry.get(key).get() instanceof Boolean booleanValue) {
                return booleanValue;
            }
            return fallback;
        }

        /**
         * Reads an integer config value by key.
         *
         * @param key the config key to read
         * @param fallback the value to return if the key is missing or not an integer
         * @return the config value, or {@code fallback}
         */
        public int getInteger(String key, int fallback) {
            if (this.valuesRegistry.containsKey(key) && this.valuesRegistry.get(key).get() instanceof Integer integerValue) {
                return integerValue;
            }
            return fallback;
        }

        /**
         * Reads a list-of-strings config value by key, filtering out any
         * non-string elements.
         *
         * @param key the config key to read
         * @return the config value as a list of strings, or an empty list if missing or not a list
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
     * Holds the faction-to-origin mapping loaded from {@code factions.json}.
     * Unlike the TOML-backed containers, this is a free-form JSON map so
     * players can freely add, remove, or rename factions without needing
     * fixed config keys.
     */
    public static class FactionsConfigContainer {

        /** The currently loaded faction-to-origin mapping. */
        private final Map<String, List<String>> factionsCache = new LinkedHashMap<>();

        /** The default faction/origin data used to seed a fresh {@code factions.json}. */
        public final FactionConfigModel defaultModelData = new FactionConfigModel();

        /**
         * Returns every origin ID assigned to any faction, flattened into a
         * single list.
         *
         * @return all origin IDs across all loaded factions
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
         * Returns the live faction-to-origin mapping.
         *
         * @return the map of faction name to its assigned origin IDs
         */
        public Map<String, List<String>> getFactionsMap() {
            return this.factionsCache;
        }

        /**
         * Replaces the cached faction-to-origin mapping. Used by the config
         * loader when {@code factions.json} is read.
         *
         * @param freshMap the new mapping to cache
         */
        protected void updateCache(Map<String, List<String>> freshMap) {
            this.factionsCache.clear();
            this.factionsCache.putAll(freshMap);
        }
    }

    /**
     * Called once the client/common TOML configs have finished loading.
     * Applies the debug logging setting and loads (or creates, on first
     * launch) {@code factions.json}.
     */
    public static void onConfigLoad() {
        try {
            if (COMMON_SPEC.isLoaded()) {
                LogManager.setDebugEnabled(COMMON.getBoolean("enableDebugLogging", false));
                FactionManager.trimAllActivityLogsToCap();
            }

            if (COMMON_SPEC.isLoaded()) {
                File subDir = new File(FMLPaths.CONFIGDIR.get().toFile(), "townsteadfactions");
                if (!subDir.exists()) {
                    subDir.mkdirs();
                }

                File jsonFile = new File(subDir, "factions.json");
                Gson gson = new GsonBuilder().setPrettyPrinting().create();

                if (!jsonFile.exists()) {
                    LogManager.info("First launch context captured. Writing custom indented factions.json structure...");
                    try (FileWriter writer = new FileWriter(jsonFile)) {
                        gson.toJson(FACTIONS.defaultModelData.factions, writer);
                    }
                    FACTIONS.updateCache(FACTIONS.defaultModelData.factions);
                } else {
                    try (FileReader reader = new FileReader(jsonFile)) {
                        Type expectedMapSignature = new TypeToken<Map<String, List<String>>>(){}.getType();
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