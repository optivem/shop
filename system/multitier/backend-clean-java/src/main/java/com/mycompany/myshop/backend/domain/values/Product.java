package com.mycompany.myshop.backend.domain.values;

import com.mycompany.myshop.backend.domain.Guard;

public class Product {

    private final Sku sku;
    private final Money price;

    public Product(Sku sku, Money price) {
        Guard.notNull(sku, Sku.FIELD_NAME);
        Guard.notNull(price, "price");
        this.sku = sku;
        this.price = price;
    }

    public Sku getSku() {
        return sku;
    }

    public Money getPrice() {
        return price;
    }
}
