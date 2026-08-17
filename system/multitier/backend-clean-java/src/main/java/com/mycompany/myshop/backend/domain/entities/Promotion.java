package com.mycompany.myshop.backend.domain.entities;

import com.mycompany.myshop.backend.domain.values.Rate;

/**
 * A promotion as the domain understands it. {@code discount} is the multiplicative factor applied
 * to the base price when the promotion is active; an inactive promotion leaves the price untouched.
 */
public class Promotion {

    private final boolean active;
    private final Rate discount;

    public Promotion(boolean active, Rate discount) {
        this.active = active;
        this.discount = discount;
    }

    public static Promotion inactive() {
        return new Promotion(false, Rate.ONE);
    }

    public boolean isActive() {
        return active;
    }

    public Rate getDiscount() {
        return discount;
    }

    /**
     * The factor to multiply the base price by: the promotion's discount when active, otherwise 1.
     */
    public Rate factor() {
        return active ? discount : Rate.ONE;
    }
}
