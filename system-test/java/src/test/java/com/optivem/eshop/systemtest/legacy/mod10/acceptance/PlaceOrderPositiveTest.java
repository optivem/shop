package com.optivem.eshop.systemtest.legacy.mod10.acceptance;

import com.optivem.eshop.systemtest.legacy.mod10.acceptance.base.BaseAcceptanceTest;
import com.optivem.eshop.dsl.channel.ChannelType;
import com.optivem.eshop.dsl.driver.port.shop.dtos.OrderStatus;
import com.optivem.testing.Channel;

import org.junit.jupiter.api.TestTemplate;

class PlaceOrderPositiveTest extends BaseAcceptanceTest {
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldPlaceOrderForValidInput() {
        scenario
                .given().product()
                    .withUnitPrice(20.00)
                .when().placeOrder()
                    .withQuantity(5)
                .then().shouldSucceed()
                .and().order()
                    .hasOrderNumberPrefix("ORD-")
                    .hasStatus(OrderStatus.PLACED)
                    .hasTotalPriceGreaterThanZero();
    }
}
