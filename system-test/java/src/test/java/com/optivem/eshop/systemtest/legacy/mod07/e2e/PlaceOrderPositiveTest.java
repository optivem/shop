package com.optivem.eshop.systemtest.legacy.mod07.e2e;

import com.optivem.eshop.dsl.channel.ChannelType;
import com.optivem.eshop.dsl.driver.port.shop.dtos.OrderStatus;
import com.optivem.eshop.systemtest.legacy.mod07.e2e.base.BaseE2eTest;
import com.optivem.testing.Channel;
import com.optivem.testing.DataSource;
import org.junit.jupiter.api.TestTemplate;

import static com.optivem.eshop.systemtest.commons.constants.Defaults.*;

class PlaceOrderPositiveTest extends BaseE2eTest {
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldPlaceOrderWithCorrectTotalPrice() {
        app.erp().returnsProduct().sku(SKU).unitPrice(20.00).execute()
                .shouldSucceed();

        app.shop().placeOrder().orderNumber(ORDER_NUMBER).sku(SKU).quantity(5).execute()
                .shouldSucceed();

        app.shop().viewOrder().orderNumber(ORDER_NUMBER).execute()
                .shouldSucceed()
                .totalPrice(100.00);
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    @DataSource({"20.00", "5", "100.00"})
    @DataSource({"10.00", "3", "30.00"})
    @DataSource({"15.50", "4", "62.00"})
    @DataSource({"99.99", "1", "99.99"})
    void shouldPlaceOrderWithCorrectTotalPriceParameterized(String unitPrice, String quantity, String totalPrice) {
        app.erp().returnsProduct().sku(SKU).unitPrice(unitPrice).execute()
                .shouldSucceed();

        app.shop().placeOrder().orderNumber(ORDER_NUMBER).sku(SKU).quantity(quantity).execute()
                .shouldSucceed();

        app.shop().viewOrder().orderNumber(ORDER_NUMBER).execute()
                .shouldSucceed()
                .totalPrice(totalPrice);
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldPlaceOrder() {
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
                .totalPrice(100.00)
                .status(OrderStatus.PLACED)
                .totalPriceGreaterThanZero();
    }
}


