package com.mycompany.myshop.backend.domain.values;

import com.mycompany.myshop.backend.domain.Guard;

import java.util.Optional;
import java.util.UUID;

public record OrderNumber(String value) {

    public static final String FIELD_NAME = "orderNumber";

    private static final String PREFIX = "ORD-";

    public OrderNumber {
        Guard.notNullOrEmpty(value, FIELD_NAME);
    }

    public static OrderNumber of(String value) {
        return new OrderNumber(value);
    }

    // Callers that treat a missing order number as "no such order" rather than as a validation
    // failure, so they can decide the outcome instead of catching.
    public static Optional<OrderNumber> requested(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new OrderNumber(value));
    }

    public static OrderNumber generate() {
        return new OrderNumber(PREFIX + UUID.randomUUID().toString().toUpperCase());
    }

    @Override
    public String toString() {
        return value;
    }
}
