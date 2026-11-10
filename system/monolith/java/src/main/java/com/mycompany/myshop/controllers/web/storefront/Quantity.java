package com.mycompany.myshop.controllers.web.storefront;

import java.util.Optional;
import java.util.regex.Pattern;

/** Quantity rules shared by the product page and the cart (UC-007 BR-2, UC-008 BR-3). */
public final class Quantity {

    public static final int MIN = 1;
    public static final int MAX = 99;
    public static final String ERROR = "Quantity must be a whole number from 1 to 99.";

    private static final Pattern DIGITS = Pattern.compile("\\d{1,9}");

    private Quantity() {
    }

    /** The typed quantity, or empty when it is not a whole number from 1 to 99. */
    public static Optional<Integer> parse(String value) {
        var trimmed = value == null ? "" : value.trim();
        if (!DIGITS.matcher(trimmed).matches()) {
            return Optional.empty();
        }
        var quantity = Integer.parseInt(trimmed);
        return quantity >= MIN && quantity <= MAX ? Optional.of(quantity) : Optional.empty();
    }

    public static int clamp(int quantity) {
        return Math.min(MAX, Math.max(MIN, quantity));
    }
}
