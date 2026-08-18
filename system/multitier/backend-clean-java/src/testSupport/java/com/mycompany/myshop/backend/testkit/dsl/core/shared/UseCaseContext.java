package com.mycompany.myshop.backend.testkit.dsl.core.shared;

import java.util.HashMap;
import java.util.Map;

public class UseCaseContext {

    private final Map<String, String> resultMap = new HashMap<>();

    public void setResultEntry(String alias, String value) {
        ensureAliasNotNullBlank(alias);

        if (resultMap.containsKey(alias)) {
            throw new IllegalStateException("Alias already exists: " + alias);
        }

        resultMap.put(alias, value);
    }

    public String getResultValue(String alias) {
        if (isNullOrBlank(alias)) {
            return alias;
        }

        var value = resultMap.get(alias);
        return value == null ? alias : value;
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
