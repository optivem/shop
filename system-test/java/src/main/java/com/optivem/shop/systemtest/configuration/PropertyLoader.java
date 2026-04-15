package com.optivem.shop.systemtest.configuration;

import com.optivem.shop.testkit.dsl.port.ChannelMode;

public class PropertyLoader {
    private PropertyLoader() {
    }

    public static Environment getEnvironment(Environment fixedEnvironment) {
        if (fixedEnvironment != null) {
            return fixedEnvironment;
        }

        var environmentMode = getRequiredSystemProperty("environment", "local|acceptance|qa|production");
        return Environment.valueOf(environmentMode.toUpperCase());
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
