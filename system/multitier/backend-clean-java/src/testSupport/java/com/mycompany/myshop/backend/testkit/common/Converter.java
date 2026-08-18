package com.mycompany.myshop.backend.testkit.common;

import java.math.BigDecimal;

public class Converter {

    private Converter() {
        throw new IllegalStateException("Utility class");
    }

    public static String fromDouble(double value) {
        return BigDecimal.valueOf(value).toPlainString();
    }
}
