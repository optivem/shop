package com.optivem.eshop.dsl.core.shared;

import com.optivem.eshop.dsl.port.ExternalSystemMode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UseCaseContext {
    private final ExternalSystemMode externalSystemMode;
    private final Map<String, String> paramMap;
    private final Map<String, String> resultMap;

    public UseCaseContext(ExternalSystemMode externalSystemMode) {
        this.externalSystemMode = externalSystemMode;
        this.paramMap = new HashMap<>();
        this.resultMap = new HashMap<>();
    }

    public ExternalSystemMode getExternalSystemMode() {
        return externalSystemMode;
    }

    public String getParamValue(String alias) {
        if(isNullOrBlank(alias)) {
            return alias;
        }

        if (paramMap.containsKey(alias)) {
            return paramMap.get(alias);
        }

        var value = generateParamValue(alias);
        paramMap.put(alias, value);

        return value;
    }

    public String getParamValueOrLiteral(String alias) {
        if(isNullOrBlank(alias)) {
            return alias;
        }

        return switch (externalSystemMode) {
            case STUB -> getParamValue(alias);
            case REAL -> alias;
            default -> throw new IllegalStateException("Unsupported external system mode: " + externalSystemMode);
        };
    }

    public void setResultEntry(String alias, String value) {
        ensureAliasNotNullBlank(alias);

        if (resultMap.containsKey(alias)) {
            throw new IllegalStateException("Alias already exists: " + alias);
        }

        resultMap.put(alias, value);
    }

    public void setResultEntryFailed(String alias, String errorMessage) {
        ensureAliasNotNullBlank(alias);
        setResultEntry(alias, "FAILED: " + errorMessage);
    }

    public String getResultValue(String alias) {
        if(isNullOrBlank(alias)) {
            return alias;
        }

        var value = resultMap.get(alias);
        if (value == null) {
            return alias; // Return literal value if not found as alias
        }

        if(value.contains("FAILED")) {
            throw new IllegalStateException("Cannot get result value for alias '" + alias + "' because the operation failed: " + value);
        }

        return value;
    }

    public String expandAliases(String message) {
        var expandedMessage = expandAlias(message, paramMap);
        expandedMessage = expandAlias(expandedMessage, resultMap);
        return expandedMessage;
    }

    private static String expandAlias(String message, Map<String, String> map) {
        var expandedMessage = message;
        for (var entry : map.entrySet()) {
            var alias = entry.getKey();
            var actualValue = entry.getValue();
            expandedMessage = expandedMessage.replace(alias, actualValue);
        }
        return expandedMessage;
    }

    private static String generateParamValue(String alias) {
        ensureAliasNotNullBlank(alias);
        var suffix = UUID.randomUUID().toString().substring(0, 8);
        return alias + "-" + suffix;
    }

    private static void ensureAliasNotNullBlank(String alias) {
        if (isNullOrBlank(alias)) {
            throw new IllegalArgumentException("Alias cannot be null or blank");
        }
    }

    private static boolean isNullOrBlank(String alias) {
        return alias == null || alias.isBlank();
    }
}




