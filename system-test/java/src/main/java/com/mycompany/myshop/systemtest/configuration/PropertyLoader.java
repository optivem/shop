package com.mycompany.myshop.systemtest.configuration;

import com.mycompany.myshop.testkit.dsl.port.ChannelMode;
import com.mycompany.myshop.testkit.dsl.port.ExternalSystemMode;

public class PropertyLoader {
    private PropertyLoader() {
    }

    public static Environment getEnvironment(Environment fixedEnvironment) {
        if (fixedEnvironment != null) {
            return fixedEnvironment;
        }

        // Resolve in order: -Denvironment system property (used by JSON commands like
        // gradle test -Denvironment=local), then ENVIRONMENT env var (set at workflow
        // shell level for acceptance/qa stages), then default LOCAL. This lets one
        // tests-*.json drive every environment without per-suite -D wiring.
        var fromProperty = System.getProperty("environment");
        if (fromProperty != null && !fromProperty.isBlank()) {
            return Environment.valueOf(fromProperty.toUpperCase());
        }
        var fromEnv = System.getenv("ENVIRONMENT");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return Environment.valueOf(fromEnv.toUpperCase());
        }
        return Environment.LOCAL;
    }

    public static ExternalSystemMode getExternalSystemMode(ExternalSystemMode fixedExternalSystemMode) {
        if (fixedExternalSystemMode != null) {
            return fixedExternalSystemMode;
        }

        var externalSystemMode = getRequiredSystemProperty("externalSystemMode", "stub|real");
        return ExternalSystemMode.valueOf(externalSystemMode.toUpperCase());
    }

    public static ChannelMode getChannelMode(ChannelMode fixedChannelMode) {
        if (fixedChannelMode != null) {
            return fixedChannelMode;
        }

        var value = System.getProperty("channelMode");
        if (value == null || value.isBlank()) {
            return ChannelMode.DYNAMIC;
        }
        return ChannelMode.valueOf(value.toUpperCase());
    }

    private static String getRequiredSystemProperty(String propertyName, String allowedValues) {
        var value = System.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    String.format("System property '%s' is not defined. Please specify -D%s=<%s>",
                            propertyName, propertyName, allowedValues)
            );
        }
        return value;
    }
}
