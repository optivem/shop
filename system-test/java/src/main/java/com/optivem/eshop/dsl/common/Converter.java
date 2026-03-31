package com.optivem.eshop.dsl.common;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Function;

public class Converter {
    private Converter() {
        throw new IllegalStateException("Utility class");
    }

    public static BigDecimal toBigDecimal(String value) {
        return from(value, BigDecimal::new);
    }

    public static BigDecimal toBigDecimal(double value) {
        return BigDecimal.valueOf(value);
    }

    public static String fromBigDecimal(BigDecimal value) {
        return from(value, BigDecimal::toString);
    }

    public static String fromDouble(double value) {
        return BigDecimal.valueOf(value).toString();
    }

    public static Integer toInteger(String value) {
        return toInteger(value, new String[0]);
    }

    public static Integer toInteger(String value, String... nullValues) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        for (String nullValue : nullValues) {
            if (value.equalsIgnoreCase(nullValue)) {
                return null;
            }
        }

        return to(value, Integer::parseInt);
    }

    public static String fromInteger(Integer value) {
        return from(value, Object::toString);
    }

    public static Double toDouble(String value) {
        return to(value, Double::parseDouble);
    }

    public static String fromInstant(Instant value) {
        return from(value, Instant::toString);
    }

    public static Instant toInstant(String text, String... nullValues) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        for (String nullValue : nullValues) {
            if (text.equalsIgnoreCase(nullValue)) {
                return null;
            }
        }
        
        // Try ISO format first
        try {
            return Instant.parse(text);
        } catch (Exception ignored) {
            // Fall through to try locale-specific formats
        }
        
        // Try various locale-specific formats that JavaScript's toLocaleString() might produce
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ofPattern("M/d/yyyy, h:mm:ss a", Locale.US),
            DateTimeFormatter.ofPattern("d/M/yyyy, HH:mm:ss", Locale.UK),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ss a", Locale.US),
        };
        
        for (DateTimeFormatter formatter : formatters) {
            try {
                LocalDateTime localDateTime = LocalDateTime.parse(text, formatter);
                return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
            } catch (Exception ignored) {
                // Try next formatter
            }
        }
        
        throw new IllegalArgumentException("Invalid date format: " + text + " - Expected ISO format or locale-specific format.");
    }

    private static <T, R> R from(T value, Function<T, R> converter) {
        return value == null ? null : converter.apply(value);
    }

    private static <T> T to(String value, Function<String, T> converter) {
        return (value == null || value.isEmpty()) ? null : converter.apply(value);
    }
}

