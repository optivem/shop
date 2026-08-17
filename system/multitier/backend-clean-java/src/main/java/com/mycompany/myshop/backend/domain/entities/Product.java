package com.mycompany.myshop.backend.domain.entities;

import com.mycompany.myshop.backend.domain.Guard;
import com.mycompany.myshop.backend.domain.values.Money;

/**
 * A product as the domain understands it: an identifier and a unit price. The ERP's wire
 * representation lives in {@code infrastructure.external.erp} and is mapped to this by the gateway
 * adapter, so a supplier renaming a JSON field cannot reach the centre.
 */
public class Product {

    private final String id;
    private final Money price;

    public Product(String id, Money price) {
        Guard.notNull(price, "price");
        this.id = id;
        this.price = price;
    }

    public String getId() {
        return id;
    }

    public Money getPrice() {
        return price;
    }
}
