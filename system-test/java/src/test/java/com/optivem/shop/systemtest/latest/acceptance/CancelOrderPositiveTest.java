package com.optivem.shop.systemtest.latest.acceptance;

import com.optivem.shop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.optivem.shop.dsl.channel.ChannelType;
import com.optivem.shop.dsl.driver.port.shop.dtos.OrderStatus;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.TestTemplate;

class CancelOrderPositiveTest extends BaseAcceptanceTest {
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldHaveCancelledStatusWhenCancelled() {
        scenario
                .given().order()
                .when().cancelOrder()
                .then().shouldSucceed()
                .and().order()
                    .hasStatus(OrderStatus.CANCELLED);
    }
}
