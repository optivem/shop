package com.optivem.eshop.systemtest.legacy.mod07.e2e;

import com.optivem.eshop.dsl.channel.ChannelType;
import com.optivem.eshop.dsl.driver.port.shop.dtos.OrderStatus;
import com.optivem.eshop.systemtest.legacy.mod07.e2e.base.BaseE2eTest;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.TestTemplate;

import static com.optivem.eshop.systemtest.commons.constants.Defaults.*;

class PlaceOrderPositiveTest extends BaseE2eTest {
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldPlaceOrderForValidInput() {
        app.erp().returnsProduct().sku(SKU).unitPrice(20.00).execute()
                .shouldSucceed();

        app.shop().placeOrder().orderNumber(ORDER_NUMBER).sku(SKU).quantity(5).execute()
                .shouldSucceed()
                .orderNumber(ORDER_NUMBER)
                .orderNumberStartsWith("ORD-");

        app.shop().viewOrder().orderNumber(ORDER_NUMBER).execute()
                .shouldSucceed()
                .orderNumber(ORDER_NUMBER)
                .sku(SKU)
                .quantity(5)
                .unitPrice(20.00)
                .status(OrderStatus.PLACED)
                .totalPriceGreaterThanZero();
    }
}
