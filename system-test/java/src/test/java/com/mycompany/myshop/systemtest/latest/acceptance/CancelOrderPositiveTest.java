package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.mycompany.myshop.testkit.common.domain.OrderStatus;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;

class CancelOrderPositiveTest extends BaseAcceptanceTest {
    @Tag("UC-003.1")
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

    @Tag("UC-003.4")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void adminShouldBeAbleToCancelCustomerOrder() {
        scenario
                .given().order()
                    .placedByCustomer()
                .and().loggedInAsAdmin()
                .when().cancelOrder()
                .then().shouldSucceed()
                .and().order()
                    .hasStatus(OrderStatus.CANCELLED);
    }

    @Tag("UC-003.2")
    @TestTemplate
    @Channel(ChannelType.UI)
    void shouldOfferToCancelAPlacedOrder() {
        scenario
                .given().order()
                .when().viewOrder()
                .then().shouldSucceed()
                .and().order()
                    .offersCancelOrder();
    }
}
