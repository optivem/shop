package com.optivem.eshop.systemtest.legacy.mod10.acceptance;

import com.optivem.eshop.systemtest.legacy.mod10.acceptance.base.BaseAcceptanceTest;
import com.optivem.eshop.dsl.channel.ChannelType;
import com.optivem.testing.Channel;
import com.optivem.testing.Isolated;
import org.junit.jupiter.api.TestTemplate;

@Isolated
class PlaceOrderNegativeIsolatedTest extends BaseAcceptanceTest {
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldRejectOrderPlacedAtYearEnd() {
        scenario
                .given().clock()
                    .withTime("2026-12-31T23:59:30Z")
                .when().placeOrder()
                .then().shouldFail()
                    .errorMessage("Orders cannot be placed between 23:59 and 00:00 on December 31st");
    }
}


