package com.optivem.eshop.systemtest.latest.acceptance;

import com.optivem.eshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.optivem.eshop.dsl.channel.ChannelType;
import com.optivem.eshop.dsl.driver.port.shop.dtos.OrderStatus;
import com.optivem.testing.Channel;

import org.junit.jupiter.api.TestTemplate;

class PlaceOrderPositiveTest extends BaseAcceptanceTest {
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void orderNumberShouldStartWithORD() {
        scenario
                .when().placeOrder()
                .then().shouldSucceed()
                .and().order()
                    .hasOrderNumberPrefix("ORD-");
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void orderStatusShouldBePlacedAfterPlacingOrder() {
        scenario
                .when().placeOrder()
                .then().shouldSucceed()
                .and().order()
                    .hasStatus(OrderStatus.PLACED);
    }
}
