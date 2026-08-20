package com.mycompany.myshop.backend.domain.values;

import com.mycompany.myshop.backend.domain.Guard;

// A product as the ERP reports it: a SKU and a price, neither of which MyShop owns or changes. A
// record like every other value here, so two readings of the same product compare equal -- which a
// hand-written class without equals could not do, and which the parity contract tests rely on when
// they compare what the stub returned against what the real system did.
public record Product(Sku sku, Money price) {

    public Product {
        Guard.notNull(sku, Sku.FIELD_NAME);
        Guard.notNull(price, "price");
    }
}
