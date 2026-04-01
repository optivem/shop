package com.optivem.eshop.systemtest.legacy.mod06.e2e;

import com.optivem.eshop.dsl.driver.port.external.erp.dtos.ReturnsProductRequest;
import com.optivem.eshop.dsl.channel.ChannelType;
import com.optivem.eshop.dsl.driver.port.shop.dtos.OrderStatus;
import com.optivem.eshop.dsl.driver.port.shop.dtos.PlaceOrderRequest;
import com.optivem.eshop.systemtest.legacy.mod06.e2e.base.BaseE2eTest;
import com.optivem.testing.Channel;

import org.junit.jupiter.api.TestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static com.optivem.eshop.dsl.common.ResultAssert.assertThatResult;
import static com.optivem.eshop.systemtest.commons.constants.Defaults.SKU;
import static org.assertj.core.api.Assertions.assertThat;

class PlaceOrderPositiveTest extends BaseE2eTest {
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldPlaceOrderForValidInput() {
        // GivenStage
        var sku = SKU + "-" + UUID.randomUUID().toString().substring(0, 8);
        var returnsProductRequest = ReturnsProductRequest.builder()
                .sku(sku)
                .price("20.00")
                .build();

        var returnsProductResult = erpDriver.returnsProduct(returnsProductRequest);
        assertThatResult(returnsProductResult).isSuccess();

        // WhenStage
        var placeOrderRequest = PlaceOrderRequest.builder()
                .sku(sku)
                .quantity("5")
                .build();

        var placeOrderResult = shopDriver.placeOrder(placeOrderRequest);
        assertThatResult(placeOrderResult).isSuccess();

        var orderNumber = placeOrderResult.getValue().getOrderNumber();
        assertThat(orderNumber).startsWith("ORD-");

        // ThenStage
        var viewOrderResult = shopDriver.viewOrder(orderNumber);
        assertThatResult(viewOrderResult).isSuccess();

        var order = viewOrderResult.getValue();
        assertThat(order.getOrderNumber()).isEqualTo(orderNumber);
        assertThat(order.getSku()).isEqualTo(sku);
        assertThat(order.getQuantity()).isEqualTo(5);
        assertThat(order.getUnitPrice()).isEqualTo(new BigDecimal("20.00"));
        assertThat(order.getTotalPrice()).isGreaterThan(BigDecimal.ZERO);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
    }
}
