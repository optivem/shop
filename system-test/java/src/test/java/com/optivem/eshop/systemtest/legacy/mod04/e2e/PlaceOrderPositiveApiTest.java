package com.optivem.eshop.systemtest.legacy.mod04.e2e;

import com.optivem.eshop.systemtest.legacy.mod04.e2e.base.BaseE2eTest;
import com.optivem.eshop.dsl.driver.adapter.external.erp.client.dtos.ExtCreateProductRequest;
import com.optivem.eshop.dsl.driver.port.shop.dtos.OrderStatus;
import com.optivem.eshop.dsl.driver.port.shop.dtos.PlaceOrderRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static com.optivem.eshop.dsl.common.ResultAssert.assertThatResult;
import static com.optivem.eshop.systemtest.commons.constants.Defaults.*;
import static org.assertj.core.api.Assertions.assertThat;

class PlaceOrderPositiveApiTest extends BaseE2eTest {
    @Override
    protected void setShopDriver() {
        setUpShopApiClient();
    }

    @Test
    void shouldPlaceOrderWithCorrectSubtotalPrice() {
        // GivenStage
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

        // WhenStage
        var placeOrderRequest = PlaceOrderRequest.builder()
                .sku(sku)
                .quantity("5")
                .country(COUNTRY)
                .build();

        var placeOrderResult = shopApiClient.orders().placeOrder(placeOrderRequest);
        assertThatResult(placeOrderResult).isSuccess();

        var orderNumber = placeOrderResult.getValue().getOrderNumber();

        // ThenStage
        var viewOrderResult = shopApiClient.orders().viewOrder(orderNumber);
        assertThatResult(viewOrderResult).isSuccess();

        var order = viewOrderResult.getValue();
        assertThat(order.getSubtotalPrice()).isEqualTo(new BigDecimal("100.00"));
    }

    @ParameterizedTest
    @CsvSource({
            "20.00, 5, 100.00",
            "10.00, 3, 30.00",
            "15.50, 4, 62.00",
            "99.99, 1, 99.99"
    })
    void shouldPlaceOrderWithCorrectSubtotalPriceParameterized(String unitPrice, String quantity, String expectedSubtotalPrice) {
        // GivenStage
        var sku = createUniqueSku(SKU);
        var createProductRequest = ExtCreateProductRequest.builder()
                .id(sku)
                .title("Test Product")
                .description("Test Description")
                .category("Test Category")
                .brand("Test Brand")
                .price(unitPrice)
                .build();

        var createProductResult = erpClient.createProduct(createProductRequest);
        assertThatResult(createProductResult).isSuccess();

        // WhenStage
        var placeOrderRequest = PlaceOrderRequest.builder()
                .sku(sku)
                .quantity(quantity)
                .country(COUNTRY)
                .build();

        var placeOrderResult = shopApiClient.orders().placeOrder(placeOrderRequest);
        assertThatResult(placeOrderResult).isSuccess();

        var orderNumber = placeOrderResult.getValue().getOrderNumber();

        // ThenStage
        var viewOrderResult = shopApiClient.orders().viewOrder(orderNumber);
        assertThatResult(viewOrderResult).isSuccess();

        var order = viewOrderResult.getValue();
        assertThat(order.getSubtotalPrice()).isEqualTo(new BigDecimal(expectedSubtotalPrice));
    }

    @Test
    void shouldPlaceOrder() {
        // GivenStage
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

        // WhenStage
        var placeOrderRequest = PlaceOrderRequest.builder()
                .sku(sku)
                .quantity("5")
                .country(COUNTRY)
                .build();

        var placeOrderResult = shopApiClient.orders().placeOrder(placeOrderRequest);
        assertThatResult(placeOrderResult).isSuccess();

        var orderNumber = placeOrderResult.getValue().getOrderNumber();
        assertThat(orderNumber).startsWith("ORD-");

        // ThenStage
        var viewOrderResult = shopApiClient.orders().viewOrder(orderNumber);
        assertThatResult(viewOrderResult).isSuccess();

        var order = viewOrderResult.getValue();
        assertThat(order.getOrderNumber()).isEqualTo(orderNumber);
        assertThat(order.getSku()).isEqualTo(sku);
        assertThat(order.getCountry()).isEqualTo(COUNTRY);
        assertThat(order.getQuantity()).isEqualTo(5);
        assertThat(order.getUnitPrice()).isEqualTo(new BigDecimal("20.00"));
        assertThat(order.getSubtotalPrice()).isEqualTo(new BigDecimal("100.00"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(order.getDiscountRate()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(order.getDiscountAmount()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(order.getSubtotalPrice()).isGreaterThan(BigDecimal.ZERO);
        assertThat(order.getTaxRate()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(order.getTaxAmount()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(order.getTotalPrice()).isGreaterThan(BigDecimal.ZERO);
    }
}


