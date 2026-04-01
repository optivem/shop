package com.optivem.eshop.systemtest.legacy.mod04.e2e;

import com.optivem.eshop.systemtest.legacy.mod04.e2e.base.BaseE2eTest;
import com.optivem.eshop.dsl.driver.adapter.external.erp.client.dtos.ExtCreateProductRequest;
import com.optivem.eshop.dsl.driver.adapter.shop.ui.client.pages.NewOrderPage;
import com.optivem.eshop.dsl.driver.port.shop.dtos.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.optivem.eshop.dsl.common.ResultAssert.assertThatResult;
import static com.optivem.eshop.systemtest.commons.constants.Defaults.COUNTRY;
import static com.optivem.eshop.systemtest.commons.constants.Defaults.SKU;
import static org.assertj.core.api.Assertions.assertThat;

class ViewOrderPositiveUiTest extends BaseE2eTest {
    @Override
    protected void setShopDriver() {
        setUpShopUiClient();
    }

    @Test
    void shouldViewPlacedOrder() {
        // GivenStage - Create product and place order
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

        var homePage = shopUiClient.openHomePage();
        var newOrderPage = homePage.clickNewOrder();
        newOrderPage.inputSku(sku);
        newOrderPage.inputQuantity("5");
        newOrderPage.inputCountry(COUNTRY);
        newOrderPage.clickPlaceOrder();

        var placeOrderResult = newOrderPage.getResult();
        assertThatResult(placeOrderResult).isSuccess();

        var orderNumber = NewOrderPage.getOrderNumber(placeOrderResult.getValue());

        // WhenStage - View order using UI pages (navigate back to home first, then to order history)
        var orderHistoryPage = shopUiClient.openHomePage().clickOrderHistory();
        orderHistoryPage.inputOrderNumber(orderNumber);
        orderHistoryPage.clickSearch();
        assertThat(orderHistoryPage.isOrderListed(orderNumber)).isTrue();

        var orderDetailsPage = orderHistoryPage.clickViewOrderDetails(orderNumber);

        // ThenStage
        assertThat(orderDetailsPage.getOrderNumber()).isEqualTo(orderNumber);
        assertThat(orderDetailsPage.getSku()).isEqualTo(sku);
        assertThat(orderDetailsPage.getCountry()).isEqualTo(COUNTRY);
        assertThat(orderDetailsPage.getQuantity()).isEqualTo(5);
        assertThat(orderDetailsPage.getUnitPrice()).isEqualTo(new BigDecimal("20.00"));
        assertThat(orderDetailsPage.getSubtotalPrice()).isEqualTo(new BigDecimal("100.00"));
        assertThat(orderDetailsPage.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(orderDetailsPage.getDiscountRate()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(orderDetailsPage.getDiscountAmount()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(orderDetailsPage.getTaxRate()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(orderDetailsPage.getTaxAmount()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(orderDetailsPage.getTotalPrice()).isGreaterThan(BigDecimal.ZERO);
    }
}


