package com.optivem.shop.systemtest.configuration;

import com.optivem.shop.testkit.dsl.port.ChannelMode;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;

public class ConfigurationLoader {
    private static final String BASE_URL = "baseUrl";

    private ConfigurationLoader() {
        throw new IllegalStateException("Utility class");
    }

    public static Configuration load(Environment environmentMode, ExternalSystemMode externalSystemMode,
                                     ChannelMode channelMode) {
        var configFile = getConfigFileName(environmentMode, externalSystemMode);
        var config = loadYamlFile(configFile);

        var shopUiBaseUrl = getNestedStringValue(config, "test", "shop", "ui", BASE_URL);
        var shopApiBaseUrl = getNestedStringValue(config, "test", "shop", "api", BASE_URL);
        var erpBaseUrl = getNestedStringValue(config, "test", "erp", "api", BASE_URL);
        var clockBaseUrl = getNestedStringValue(config, "test", "clock", "api", BASE_URL);
        var taxBaseUrl = getNestedStringValue(config, "test", "tax", "api", BASE_URL);

        return new Configuration(shopUiBaseUrl, shopApiBaseUrl, erpBaseUrl, clockBaseUrl, taxBaseUrl,
                externalSystemMode, channelMode);
    }

    private static String getConfigFileName(Environment environmentMode, ExternalSystemMode externalSystemMode) {
        var env = environmentMode.name().toLowerCase();
        var mode = externalSystemMode.name().toLowerCase();
        return String.format("test-config-%s-%s.yml", env, mode);
    }

    private static Map<String, Object> loadYamlFile(String fileName) {
        var yaml = new Yaml();
        var inputStream = ConfigurationLoader.class
                .getClassLoader()
                .getResourceAsStream(fileName);

        if (inputStream == null) {
            throw new IllegalStateException("Configuration file not found: " + fileName);
        }

        return yaml.load(inputStream);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getNestedValue(Map<String, Object> config, String... keys) {
        var current = config;
        for (int i = 0; i < keys.length - 1; i++) {
            current = (Map<String, Object>) current.get(keys[i]);
        }
        return (T) current.get(keys[keys.length - 1]);
    }

    private static String getNestedStringValue(Map<String, Object> config, String... keys) {
        return getNestedValue(config, keys);
    }
}
