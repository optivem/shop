package com.mycompany.myshop.backend.domain.entities;

import com.mycompany.myshop.backend.domain.values.Money;
import com.mycompany.myshop.backend.domain.values.Sku;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ProductTest {

    @Test
    void carriesItsSkuAndPrice() {
        var product = new Product(Sku.of("BOOK-123"), Money.of("10.00"));

        assertThat(product.getSku()).isEqualTo(Sku.of("BOOK-123"));
        assertThat(product.getPrice()).isEqualTo(Money.of("10.00"));
    }

    @Test
    void rejectsConstructionWithoutASku() {
        var thrown = catchThrowable(() -> new Product(null, Money.of("10.00")));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("sku cannot be null");
    }

    @Test
    void rejectsConstructionWithoutAPrice() {
        var thrown = catchThrowable(() -> new Product(Sku.of("BOOK-123"), null));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("price cannot be null");
    }
}
