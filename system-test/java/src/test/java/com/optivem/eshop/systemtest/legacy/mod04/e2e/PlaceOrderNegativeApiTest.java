package com.optivem.eshop.systemtest.legacy.mod04.e2e;

import com.optivem.eshop.systemtest.legacy.mod04.e2e.base.BaseE2eTest;
import com.optivem.eshop.dsl.driver.adapter.external.erp.client.dtos.ExtCreateProductRequest;
import com.optivem.eshop.dsl.driver.port.shop.dtos.PlaceOrderRequest;
import org.junit.jupiter.api.Test;

import static com.optivem.eshop.dsl.common.ResultAssert.assertThatResult;
import static com.optivem.eshop.systemtest.commons.constants.Defaults.*;
import static org.assertj.core.api.Assertions.assertThat;

class PlaceOrderNegativeApiTest extends BaseE2eTest {
    @Override
    protected void setShopDriver() {
        setUpShopApiClient();
    }

    @Test
    void shouldRejectOrderWithInvalidQuantity() {
        var placeOrderRequest = PlaceOrderRequest.builder()
                .sku(createUniqueSku(SKU))
                .quantity("invalid-quantity")
                .country(COUNTRY)
                .build();

        var placeOrderResult = shopApiClient.orders().placeOrder(placeOrderRequest);

        assertThatResult(placeOrderResult).isFailure();
        var error = placeOrderResult.getError();
        assertThat(error.getDetail()).isEqualTo("The request contains one or more validation errors");
        assertThat(error.getErrors()).anySatisfy(field -> {
            assertThat(field.getField()).isEqualTo("quantity");
            assertThat(field.getMessage()).isEqualTo("Quantity must be an integer");
        });
    }

    @Test
    void shouldRejectOrderWithNonExistentSku() {
        var placeOrderRequest = PlaceOrderRequest.builder()
                .sku("NON-EXISTENT-SKU-12345")
                .quantity(QUANTITY)
                .country(COUNTRY)
                .build();

        var placeOrderResult = shopApiClient.orders().placeOrder(placeOrderRequest);

        assertThatResult(placeOrderResult).isFailure();
        var error = placeOrderResult.getError();
        assertThat(error.getDetail()).isEqualTo("The request contains one or more validation errors");
        assertThat(error.getErrors()).anySatisfy(field -> {
            assertThat(field.getField()).isEqualTo("sku");
            assertThat(field.getMessage()).isEqualTo("Product does not exist for SKU: NON-EXISTENT-SKU-12345");
        });
    }

    @Test
    void shouldRejectOrderWithNegativeQuantity() {
        var placeOrderRequest = PlaceOrderRequest.builder()
                .sku(createUniqueSku(SKU))
                .quantity("-10")
                .country(COUNTRY)
                .build();

        var placeOrderResult = shopApiClient.orders().placeOrder(placeOrderRequest);

        assertThatResult(placeOrderResult).isFailure();
        var error = placeOrderResult.getError();
        assertThat(error.getDetail()).isEqualTo("The request contains one or more validation errors");
        assertThat(error.getErrors()).anySatisfy(field -> {
            assertThat(field.getField()).isEqualTo("quantity");
            assertThat(field.getMessage()).isEqualTo("Quantity must be positive");
        });
    }

    @Test
    void shouldRejectOrderWithZeroQuantity() {
        var placeOrderRequest = PlaceOrderRequest.builder()
                .sku(createUniqueSku(SKU))
                .quantity("0")
                .country(COUNTRY)
                .build();

        var placeOrderResult = shopApiClient.orders().placeOrder(placeOrderRequest);

        assertThatResult(placeOrderResult).isFailure();
        var error = placeOrderResult.getError();
        assertThat(error.getDetail()).isEqualTo("The request contains one or more validation errors");
        assertThat(error.getErrors()).anySatisfy(field -> {
            assertThat(field.getField()).isEqualTo("quantity");
            assertThat(field.getMessage()).isEqualTo("Quantity must be positive");
        });
    }

    @Test
    void shouldRejectOrderWithEmptySku() {
        var placeOrderRequest = PlaceOrderRequest.builder()
                .sku("")
                .quantity(QUANTITY)
                .country(COUNTRY)
                .build();

        var placeOrderResult = shopApiClient.orders().placeOrder(placeOrderRequest);

        assertThatResult(placeOrderResult).isFailure();
        var error = placeOrderResult.getError();
        assertThat(error.getDetail()).isEqualTo("The request contains one or more validation errors");
        assertThat(error.getErrors()).anySatisfy(field -> {
            assertThat(field.getField()).isEqualTo("sku");
            assertThat(field.getMessage()).isEqualTo("SKU must not be empty");
        });
    }

    @Test
    void shouldRejectOrderWithEmptyQuantity() {
        var placeOrderRequest = PlaceOrderRequest.builder()
                .sku(createUniqueSku(SKU))
                .quantity("")
                .country(COUNTRY)
                .build();

        var placeOrderResult = shopApiClient.orders().placeOrder(placeOrderRequest);

        assertThatResult(placeOrderResult).isFailure();
        var error = placeOrderResult.getError();
        assertThat(error.getDetail()).isEqualTo("The request contains one or more validation errors");
        assertThat(error.getErrors()).anySatisfy(field -> {
            assertThat(field.getField()).isEqualTo("quantity");
            assertThat(field.getMessage()).isEqualTo("Quantity must not be empty");
        });
    }

    @Test
    void shouldRejectOrderWithNonIntegerQuantity() {
        var placeOrderRequest = PlaceOrderRequest.builder()
                .sku(createUniqueSku(SKU))
                .quantity("3.5")
                .country(COUNTRY)
                .build();

        var placeOrderResult = shopApiClient.orders().placeOrder(placeOrderRequest);

        assertThatResult(placeOrderResult).isFailure();
        var error = placeOrderResult.getError();
        assertThat(error.getDetail()).isEqualTo("The request contains one or more validation errors");
        assertThat(error.getErrors()).anySatisfy(field -> {
            assertThat(field.getField()).isEqualTo("quantity");
            assertThat(field.getMessage()).isEqualTo("Quantity must be an integer");
        });
    }

    @Test
    void shouldRejectOrderWithEmptyCountry() {
        var placeOrderRequest = PlaceOrderRequest.builder()
                .sku(createUniqueSku(SKU))
                .quantity(QUANTITY)
                .country("")
                .build();

        var placeOrderResult = shopApiClient.orders().placeOrder(placeOrderRequest);

        assertThatResult(placeOrderResult).isFailure();
        var error = placeOrderResult.getError();
        assertThat(error.getDetail()).isEqualTo("The request contains one or more validation errors");
        assertThat(error.getErrors()).anySatisfy(field -> {
            assertThat(field.getField()).isEqualTo("country");
            assertThat(field.getMessage()).isEqualTo("Country must not be empty");
        });
    }

    @Test
    void shouldRejectOrderWithInvalidCountry() {
        var sku = createUniqueSku(SKU);
        var createProductRequest = ExtCreateProductRequest.builder()
                .id(sku)
                .title("Test Product")
                .description("Test Description")
                .category("Test Category")
                .brand("Test Brand")
                .price("20.00")
                .build();

        var createProductResult = erpClient.createProduct(createProductRequest);
        assertThatResult(createProductResult).isSuccess();

        var placeOrderRequest = PlaceOrderRequest.builder()
                .sku(sku)
                .quantity(QUANTITY)
                .country("XX")
                .build();

        var placeOrderResult = shopApiClient.orders().placeOrder(placeOrderRequest);

        assertThatResult(placeOrderResult).isFailure();
        var error = placeOrderResult.getError();
        assertThat(error.getDetail()).isEqualTo("The request contains one or more validation errors");
        assertThat(error.getErrors()).anySatisfy(field -> {
            assertThat(field.getField()).isEqualTo("country");
            assertThat(field.getMessage()).isEqualTo("Country does not exist: XX");
        });
    }

    @Test
    void shouldRejectOrderWithNullQuantity() {
        var placeOrderRequest = PlaceOrderRequest.builder()
                .sku(createUniqueSku(SKU))
                .country(COUNTRY)
                .quantity(null)
                .build();

        var placeOrderResult = shopApiClient.orders().placeOrder(placeOrderRequest);

        assertThatResult(placeOrderResult).isFailure();
        var error = placeOrderResult.getError();
        assertThat(error.getDetail()).isEqualTo("The request contains one or more validation errors");
        assertThat(error.getErrors()).anySatisfy(field -> {
            assertThat(field.getField()).isEqualTo("quantity");
            assertThat(field.getMessage()).isEqualTo("Quantity must not be empty");
        });
    }

    @Test
    void shouldRejectOrderWithNullSku() {
        var placeOrderRequest = PlaceOrderRequest.builder()
                .sku(null)
                .quantity(QUANTITY)
                .country(COUNTRY)
                .build();

        var placeOrderResult = shopApiClient.orders().placeOrder(placeOrderRequest);

        assertThatResult(placeOrderResult).isFailure();
        var error = placeOrderResult.getError();
        assertThat(error.getDetail()).isEqualTo("The request contains one or more validation errors");
        assertThat(error.getErrors()).anySatisfy(field -> {
            assertThat(field.getField()).isEqualTo("sku");
            assertThat(field.getMessage()).isEqualTo("SKU must not be empty");
        });
    }

    @Test
    void shouldRejectOrderWithNullCountry() {
        var placeOrderRequest = PlaceOrderRequest.builder()
                .sku(createUniqueSku(SKU))
                .quantity(QUANTITY)
                .country(null)
                .build();

        var placeOrderResult = shopApiClient.orders().placeOrder(placeOrderRequest);

        assertThatResult(placeOrderResult).isFailure();
        var error = placeOrderResult.getError();
        assertThat(error.getDetail()).isEqualTo("The request contains one or more validation errors");
        assertThat(error.getErrors()).anySatisfy(field -> {
            assertThat(field.getField()).isEqualTo("country");
            assertThat(field.getMessage()).isEqualTo("Country must not be empty");
        });
    }
}

