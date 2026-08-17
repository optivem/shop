package com.mycompany.myshop.backend.testkit.common;

import java.math.BigDecimal;

/**
 * Conversions shared by the test DSL. Mirrors {@code system-test/java}'s converter of the same name
 * so the two testkits format numbers identically.
 */
public class Converter {

    private Converter() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Formats a {@code double} as the decimal text the DSL's {@code String} forms expect.
     *
     * <p>Uses {@code toPlainString()}, never {@code toString()}: the latter emits scientific notation
     * for small magnitudes (e.g. {@code 1.0E-7}), which the domain's decimal parsing rejects.
     */
    public static String fromDouble(double value) {
        return BigDecimal.valueOf(value).toPlainString();
    }
}
