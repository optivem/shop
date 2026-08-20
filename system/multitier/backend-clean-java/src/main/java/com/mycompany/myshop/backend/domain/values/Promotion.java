package com.mycompany.myshop.backend.domain.values;

import com.mycompany.myshop.backend.domain.Guard;

// The ERP's current promotion. A record for the same reason as Product and TaxRate, and it keeps its
// behaviour: factor() is what callers actually want, because an inactive promotion and a promotion
// that discounts nothing multiply the price identically.
public record Promotion(boolean active, Rate discount) {

    public Promotion {
        Guard.notNull(discount, "discount");
    }

    public static Promotion inactive() {
        return new Promotion(false, Rate.ONE);
    }

    public Rate factor() {
        return active ? discount : Rate.ONE;
    }
}
