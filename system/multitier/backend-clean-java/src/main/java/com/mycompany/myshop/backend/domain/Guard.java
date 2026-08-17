package com.mycompany.myshop.backend.domain;

/**
 * Argument checks shared by the entities and values — the {@code requireNotNull} helper that had
 * been copied privately into {@link com.mycompany.myshop.backend.domain.entities.Order} and
 * {@link com.mycompany.myshop.backend.domain.pricing.OrderPricing}, stated once.
 *
 * <p>Deliberately small. These are programmer errors — a caller handing the domain a null it was
 * never allowed to hand it — so they raise {@link IllegalArgumentException}, not the business
 * {@link com.mycompany.myshop.backend.domain.exceptions.ValidationException}. Business rules do not
 * belong here: a rule that reads two fields against each other, or bounds a value the domain cares
 * about, belongs on the type that owns those fields. See
 * {@link com.mycompany.myshop.backend.domain.values.ValidityPeriod} for the shape that takes.
 */
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

    /** Null passes: an absent optional value is not a negative one. */
    public static void notNegative(Integer value, String name) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }
}
