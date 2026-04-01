package com.optivem.eshop.systemtest.legacy.mod05.e2e;

import com.optivem.eshop.dsl.driver.port.shop.dtos.PlaceOrderRequest;
import org.junit.jupiter.api.Test;

import static com.optivem.eshop.dsl.common.ResultAssert.assertThatResult;
import static com.optivem.eshop.systemtest.commons.constants.Defaults.*;
import static org.assertj.core.api.Assertions.assertThat;

class PlaceOrderNegativeApiTest extends PlaceOrderNegativeBaseTest {
    @Override
    protected void setShopDriver() {
        setUpShopApiDriver();
    }

    @Test
    void shouldRejectOrderWithNullQuantity() {
        var request = PlaceOrderRequest.builder()
                .sku(createUniqueSku(SKU))
                .country(COUNTRY)
                .quantity(null)
                .build();

        var result = shopDriver.placeOrder(request);

        assertThatResult(result).isFailure();
        var error = result.getError();
        assertThat(error.getMessage()).isEqualTo("The request contains one or more validation errors");
        assertThat(error.getFields()).anySatisfy(field -> {
            assertThat(field.getField()).isEqualTo("quantity");
            assertThat(field.getMessage()).isEqualTo("Quantity must not be empty");
        });
    }

    @Test
    void shouldRejectOrderWithNullSku() {
        var request = PlaceOrderRequest.builder()
                .sku(null)
                .quantity(QUANTITY)
                .country(COUNTRY)
                .build();

        var result = shopDriver.placeOrder(request);

        assertThatResult(result).isFailure();
        var error = result.getError();
        assertThat(error.getMessage()).isEqualTo("The request contains one or more validation errors");
        assertThat(error.getFields()).anySatisfy(field -> {
            assertThat(field.getField()).isEqualTo("sku");
            assertThat(field.getMessage()).isEqualTo("SKU must not be empty");
        });
    }

    @Test
    void shouldRejectOrderWithNullCountry() {
        var request = PlaceOrderRequest.builder()
                .sku(createUniqueSku(SKU))
                .quantity(QUANTITY)
                .country(null)
                .build();

        var result = shopDriver.placeOrder(request);

        assertThatResult(result).isFailure();
        var error = result.getError();
        assertThat(error.getMessage()).isEqualTo("The request contains one or more validation errors");
        assertThat(error.getFields()).anySatisfy(field -> {
            assertThat(field.getField()).isEqualTo("country");
            assertThat(field.getMessage()).isEqualTo("Country must not be empty");
        });
    }
}

