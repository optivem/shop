package com.mycompany.myshop.backend.domain.entities;

import com.mycompany.myshop.backend.domain.Guard;
import com.mycompany.myshop.backend.domain.values.Money;

public class Product {

    private final String id;
    private final Money price;

    public Product(String id, Money price) {
        // notNull rather than notNullOrEmpty, matching how Order guards the same concept as its sku.
        Guard.notNull(id, "id");
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
