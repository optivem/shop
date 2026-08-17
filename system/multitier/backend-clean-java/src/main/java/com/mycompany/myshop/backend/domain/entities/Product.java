package com.mycompany.myshop.backend.domain.entities;

import java.math.BigDecimal;

/**
 * A product as the domain understands it: an identifier and a unit price. The ERP's wire
 * representation lives in {@code infrastructure.external.erp} and is mapped to this by the gateway
 * adapter, so a supplier renaming a JSON field cannot reach the centre.
 */
public class Product {

    private final String id;
    private final BigDecimal price;

    public Product(String id, BigDecimal price) {
        if (price == null) {
            throw new IllegalArgumentException("price cannot be null");
        }
        this.id = id;
        this.price = price;
    }

    public String getId() {
        return id;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
