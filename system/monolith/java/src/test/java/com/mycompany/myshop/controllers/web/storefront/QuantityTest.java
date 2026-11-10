package com.mycompany.myshop.controllers.web.storefront;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class QuantityTest {

    @ParameterizedTest
    @ValueSource(strings = {"1", "99", " 3 "})
    void acceptsAWholeNumberFrom1To99(String value) {
        assertThat(Quantity.parse(value)).isPresent();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "100", "-1", "1.5", "abc", "", " ", "99999999999"})
    void rejectsAnythingElse(String value) {
        assertThat(Quantity.parse(value)).isEmpty();
    }
}
