package com.mycompany.myshop.backend.domain.values;

import com.mycompany.myshop.backend.domain.Guard;

/**
 * A promotion as the domain understands it. {@code discount} is the multiplicative factor applied
 * to the base price when the promotion is active; an inactive promotion leaves the price untouched.
 *
 * <p>A value, not an entity: no identity, immutable, and two promotions with the same flag and the
 * same discount are the same promotion. It is constructed from ERP wire data in
 * {@code infrastructure.external.erp.HttpErpGateway}, which is why the discount is guarded here —
 * a null would otherwise surface as an {@code NPE} inside the pricing chain, far from its cause.
 */
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

    /**
     * The factor to multiply the base price by: the promotion's discount when active, otherwise 1.
     */
    public Rate factor() {
        return active ? discount : Rate.ONE;
    }
}
