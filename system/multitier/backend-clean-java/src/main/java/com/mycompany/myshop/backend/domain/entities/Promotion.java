package com.mycompany.myshop.backend.domain.entities;

import java.math.BigDecimal;

/**
 * A promotion as the domain understands it. {@code discount} is the multiplicative factor applied
 * to the base price when the promotion is active; an inactive promotion leaves the price untouched.
 */
public class Promotion {

    private final boolean active;
    private final BigDecimal discount;

    public Promotion(boolean active, BigDecimal discount) {
        this.active = active;
        this.discount = discount;
    }

    public static Promotion inactive() {
        return new Promotion(false, BigDecimal.ONE);
    }

    public boolean isActive() {
        return active;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    /**
     * The factor to multiply the base price by: the promotion's discount when active, otherwise 1.
     */
    public BigDecimal factor() {
        return active ? discount : BigDecimal.ONE;
    }
}
