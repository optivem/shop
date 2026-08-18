package com.mycompany.myshop.systemtest.configuration;

import com.mycompany.myshop.testkit.dsl.port.ChannelMode;
import com.mycompany.myshop.testkit.dsl.port.ExternalSystemMode;
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

        // Env var overrides let the cross-lang verification workflow point this
        // test JAR at a different language's SUT without touching the YAML files.
        // Suffix matches externalSystemMode so stub/real suites in one tests.yaml
        // run can each get their own URL set.
        var suffix = "_" + externalSystemMode.name().toUpperCase();
        var myShopUiBaseUrl = getEnvVarOrDefault("MYSHOP_UI_BASE_URL" + suffix,
                getNestedStringValue(config, "test", "myShop", "ui", BASE_URL));
        var myShopApiBaseUrl = getEnvVarOrDefault("MYSHOP_API_BASE_URL" + suffix,
                getNestedStringValue(config, "test", "myShop", "api", BASE_URL));
        var erpBaseUrl = getEnvVarOrDefault("ERP_API_BASE_URL" + suffix,
                getNestedStringValue(config, "test", "erp", "api", BASE_URL));
        var clockBaseUrl = getEnvVarOrDefault("CLOCK_API_BASE_URL" + suffix,
                getNestedStringValue(config, "test", "clock", "api", BASE_URL));
        var taxBaseUrl = getEnvVarOrDefault("TAX_API_BASE_URL" + suffix,
                getNestedStringValue(config, "test", "tax", "api", BASE_URL));

        return new Configuration(myShopUiBaseUrl, myShopApiBaseUrl, erpBaseUrl, clockBaseUrl, taxBaseUrl,
                externalSystemMode, channelMode);
    }

    private static String getEnvVarOrDefault(String envVarName, String fileValue) {
        var envValue = System.getenv(envVarName);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return fileValue;
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
