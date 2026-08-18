package com.mycompany.myshop.backend.domain.entities;

import com.mycompany.myshop.backend.domain.Guard;
import com.mycompany.myshop.backend.domain.values.Money;

public class Product {

    private final String sku;
    private final Money price;

    public Product(String sku, Money price) {
        // notNull rather than notNullOrEmpty, matching how Order guards the same concept as its sku.
        Guard.notNull(sku, "sku");
        Guard.notNull(price, "price");
        this.sku = sku;
        this.price = price;
    }

    public String getSku() {
        return sku;
    }

    public Money getPrice() {
        return price;
    }
}
