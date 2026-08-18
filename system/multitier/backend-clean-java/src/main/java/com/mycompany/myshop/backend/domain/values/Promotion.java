package com.mycompany.myshop.backend.domain.values;

import com.mycompany.myshop.backend.domain.Guard;

public class Promotion {

    private final boolean active;
    private final Rate discount;

    public Promotion(boolean active, Rate discount) {
        Guard.notNull(discount, "discount");
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

    public Rate factor() {
        return active ? discount : Rate.ONE;
    }
}
