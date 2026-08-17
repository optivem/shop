package com.mycompany.myshop.backend.domain.entities;

import com.mycompany.myshop.backend.domain.Guard;
import com.mycompany.myshop.backend.domain.values.Money;

/**
 * A product as the domain understands it: an identifier and a unit price. The ERP's wire
 * representation lives in {@code infrastructure.external.erp} and is mapped to this by the gateway
 * adapter, so a supplier renaming a JSON field cannot reach the centre.
 *
 * <p>The {@code id} stays a {@code String} where a coupon's code and an order's country became types
 * — the asymmetry is deliberate. Those two each had a rule to own: a blank code meant two opposite
 * things in different callers, and a country is the key one gateway is queried by. A product id has
 * neither. The ERP owns its format, the one entry point that accepts it already enforces
 * {@code @NotBlank} ({@code PlaceOrderRequest}), and nothing in the domain compares this id with
 * {@code Order.sku} — the sku goes out to the ERP and a price comes back, so the two never meet. A
 * {@code ProductId} here would be a type that only ever travels, which is what makes value objects
 * read as ceremony.
 */
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
