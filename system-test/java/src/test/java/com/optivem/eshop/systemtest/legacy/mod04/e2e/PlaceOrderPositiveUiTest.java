package com.optivem.eshop.systemtest.legacy.mod04.e2e;

import com.optivem.eshop.systemtest.legacy.mod04.e2e.base.BaseE2eTest;
import com.optivem.eshop.dsl.driver.adapter.external.erp.client.dtos.ExtCreateProductRequest;
import com.optivem.eshop.dsl.driver.adapter.shop.ui.client.pages.NewOrderPage;
import com.optivem.eshop.dsl.driver.port.shop.dtos.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static com.optivem.eshop.dsl.common.ResultAssert.assertThatResult;
import static com.optivem.eshop.systemtest.commons.constants.Defaults.*;
import static org.assertj.core.api.Assertions.assertThat;

class PlaceOrderPositiveUiTest extends BaseE2eTest {
    @Override
    protected void setShopClient() {
        setUpShopUiClient();
    }

    @Test
    void shouldPlaceOrderForValidInput() {
        // GivenStage
        var sku = SKU + "-" + UUID.randomUUID().toString().substring(0, 8);
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
        var homePage = shopUiClient.openHomePage();
        var newOrderPage = homePage.clickNewOrder();
        newOrderPage.inputSku(sku);
        newOrderPage.inputQuantity("5");
        newOrderPage.clickPlaceOrder();

        var placeOrderResult = newOrderPage.getResult();
        assertThatResult(placeOrderResult).isSuccess();

        var orderNumber = NewOrderPage.getOrderNumber(placeOrderResult.getValue());
        assertThat(orderNumber).startsWith("ORD-");

        // ThenStage
        var orderHistoryPage = shopUiClient.openHomePage().clickOrderHistory();
        orderHistoryPage.inputOrderNumber(orderNumber);
        orderHistoryPage.clickSearch();
        assertThat(orderHistoryPage.isOrderListed(orderNumber)).isTrue();

        var orderDetailsPage = orderHistoryPage.clickViewOrderDetails(orderNumber);
        assertThat(orderDetailsPage.getOrderNumber()).isEqualTo(orderNumber);
        assertThat(orderDetailsPage.getSku()).isEqualTo(sku);
        assertThat(orderDetailsPage.getQuantity()).isEqualTo(5);
        assertThat(orderDetailsPage.getUnitPrice()).isEqualTo(new BigDecimal("20.00"));
        assertThat(orderDetailsPage.getTotalPrice()).isEqualTo(new BigDecimal("100.00"));
        assertThat(orderDetailsPage.getStatus()).isEqualTo(OrderStatus.PLACED);
    }
}
