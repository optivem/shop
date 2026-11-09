package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.mycompany.myshop.testkit.common.domain.OrderStatus;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;

class DeliverOrderPositiveTest extends BaseAcceptanceTest {
    @Tag("UC-012.1")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldHaveDeliveredStatusWhenDelivered() {
        scenario
                .given().order()
                    .placedByCustomer()
                .and().loggedInAsAdmin()
                .when().deliverOrder()
                .then().shouldSucceed()
                .and().order()
                    .hasStatus(OrderStatus.DELIVERED);
    }

    @Tag("UC-012.3")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void adminShouldBeAbleToDeliverAnotherCustomersOrder() {
        scenario
                .given().order()
                    .placedByCustomer("B")
                .and().loggedInAsAdmin()
                .when().deliverOrder()
                .then().shouldSucceed()
                .and().order()
                    .hasStatus(OrderStatus.DELIVERED);
    }
}
