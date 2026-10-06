package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.systemtest.latest.acceptance.base.RequiresKeycloak;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.mycompany.myshop.testkit.common.domain.OrderStatus;
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

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    @RequiresKeycloak
    void adminShouldBeAbleToCancelCustomerOrder() {
        scenario
                .given().order()
                    .placedByCustomer()
                .when().actingAsAdmin().cancelOrder()
                .then().shouldSucceed()
                .and().order()
                    .hasStatus(OrderStatus.CANCELLED);
    }
}
