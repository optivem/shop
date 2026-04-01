package com.optivem.eshop.systemtest.legacy.mod05.e2e;

import com.optivem.eshop.dsl.driver.port.external.erp.dtos.ReturnsProductRequest;
import com.optivem.eshop.dsl.driver.port.shop.dtos.PlaceOrderRequest;
import com.optivem.eshop.systemtest.commons.providers.EmptyArgumentsProvider;
import com.optivem.eshop.systemtest.legacy.mod05.e2e.base.BaseE2eTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import static com.optivem.eshop.dsl.common.ResultAssert.assertThatResult;
import static com.optivem.eshop.systemtest.commons.constants.Defaults.*;
import static org.assertj.core.api.Assertions.assertThat;

abstract class PlaceOrderNegativeBaseTest extends BaseE2eTest {
    @Test
    void shouldRejectOrderWithInvalidQuantity() {
        var request = PlaceOrderRequest.builder()
                .sku(createUniqueSku(SKU))
                .quantity("invalid-quantity")
                .country(COUNTRY)
                .build();

        var result = shopDriver.placeOrder(request);

        assertThatResult(result).isFailure();
        var error = result.getError();
        assertThat(error.getMessage()).isEqualTo("The request contains one or more validation errors");
        assertThat(error.getFields()).anySatisfy(field -> {
            assertThat(field.getField()).isEqualTo("quantity");
            assertThat(field.getMessage()).isEqualTo("Quantity must be an integer");
        });
    }

    @Test
    void shouldRejectOrderWithNonExistentSku() {
        var request = PlaceOrderRequest.builder()
                .sku("NON-EXISTENT-SKU-12345")
                .quantity(QUANTITY)
                .country(COUNTRY)
                .build();

        var result = shopDriver.placeOrder(request);

        assertThatResult(result).isFailure();
        var error = result.getError();
        assertThat(error.getMessage()).isEqualTo("The request contains one or more validation errors");
        assertThat(error.getFields()).anySatisfy(field -> {
            assertThat(field.getField()).isEqualTo("sku");
            assertThat(field.getMessage()).isEqualTo("Product does not exist for SKU: NON-EXISTENT-SKU-12345");
        });
    }

    @Test
    void shouldRejectOrderWithNegativeQuantity() {
        var request = PlaceOrderRequest.builder()
                .sku(createUniqueSku(SKU))
                .quantity("-10")
                .country(COUNTRY)
                .build();

        var result = shopDriver.placeOrder(request);

        assertThatResult(result).isFailure();
        var error = result.getError();
        assertThat(error.getMessage()).isEqualTo("The request contains one or more validation errors");
        assertThat(error.getFields()).anySatisfy(field -> {
            assertThat(field.getField()).isEqualTo("quantity");
            assertThat(field.getMessage()).isEqualTo("Quantity must be positive");
        });
    }

    @Test
    void shouldRejectOrderWithZeroQuantity() {
        var request = PlaceOrderRequest.builder()
                .sku("ANOTHER-SKU-67890")
                .quantity("0")
                .country(COUNTRY)
                .build();

        var result = shopDriver.placeOrder(request);

        assertThatResult(result).isFailure();
        var error = result.getError();
        assertThat(error.getMessage()).isEqualTo("The request contains one or more validation errors");
        assertThat(error.getFields()).anySatisfy(field -> {
            assertThat(field.getField()).isEqualTo("quantity");
            assertThat(field.getMessage()).isEqualTo("Quantity must be positive");
        });
    }

    @ParameterizedTest
    @ArgumentsSource(EmptyArgumentsProvider.class)
    void shouldRejectOrderWithEmptySku(String sku) {
        var request = PlaceOrderRequest.builder()
                .sku(sku)
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
    void shouldRejectOrderWithEmptyQuantity() {
        var request = PlaceOrderRequest.builder()
                .sku(createUniqueSku(SKU))
                .quantity("")
                .country(COUNTRY)
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
    void shouldRejectOrderWithNonIntegerQuantity() {
        var request = PlaceOrderRequest.builder()
                .sku(createUniqueSku(SKU))
                .quantity("3.5")
                .country(COUNTRY)
                .build();

        var result = shopDriver.placeOrder(request);

        assertThatResult(result).isFailure();
        var error = result.getError();
        assertThat(error.getMessage()).isEqualTo("The request contains one or more validation errors");
        assertThat(error.getFields()).anySatisfy(field -> {
            assertThat(field.getField()).isEqualTo("quantity");
            assertThat(field.getMessage()).isEqualTo("Quantity must be an integer");
        });
    }

    @Test
    void shouldRejectOrderWithEmptyCountry() {
        var request = PlaceOrderRequest.builder()
                .sku(createUniqueSku(SKU))
                .quantity(QUANTITY)
                .country("")
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

    @Test
    void shouldRejectOrderWithInvalidCountry() {
        var sku = createUniqueSku(SKU);
        var returnsProductRequest = ReturnsProductRequest.builder()
                .sku(sku)
                .price("20.00")
                .build();

        var returnsProductResult = erpDriver.returnsProduct(returnsProductRequest);
        assertThatResult(returnsProductResult).isSuccess();

        var request = PlaceOrderRequest.builder()
                .sku(sku)
                .quantity(QUANTITY)
                .country("XX")
                .build();

        var result = shopDriver.placeOrder(request);

        assertThatResult(result).isFailure();
        var error = result.getError();
        assertThat(error.getMessage()).isEqualTo("The request contains one or more validation errors");
        assertThat(error.getFields()).anySatisfy(field -> {
            assertThat(field.getField()).isEqualTo("country");
            assertThat(field.getMessage()).isEqualTo("Country does not exist: XX");
        });
    }
}

