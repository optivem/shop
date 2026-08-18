package com.mycompany.myshop.backend.domain.entities;

import com.mycompany.myshop.backend.domain.values.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ProductTest {

    @Test
    void carriesItsIdentifierAndPrice() {
        var product = new Product("BOOK-123", Money.of("10.00"));

        assertThat(product.getSku()).isEqualTo("BOOK-123");
        assertThat(product.getPrice()).isEqualTo(Money.of("10.00"));
    }

    @Test
    void rejectsConstructionWithoutAnIdentifier() {
        var thrown = catchThrowable(() -> new Product(null, Money.of("10.00")));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("id cannot be null");
    }

    @Test
    void rejectsConstructionWithoutAPrice() {
        var thrown = catchThrowable(() -> new Product("BOOK-123", null));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("price cannot be null");
    }
}
