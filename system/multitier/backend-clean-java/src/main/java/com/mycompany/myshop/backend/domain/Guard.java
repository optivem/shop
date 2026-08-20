package com.mycompany.myshop.backend.domain;

public final class Guard {

    private Guard() {
    }

    public static void notNull(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
    }

    public static void notNullOrEmpty(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be null or empty");
        }
    }

    public static void positive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    public static void notNegative(Integer value, String name) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }
}
