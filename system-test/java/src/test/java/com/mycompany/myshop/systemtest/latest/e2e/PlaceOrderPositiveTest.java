package com.mycompany.myshop.systemtest.latest.e2e;

import com.mycompany.myshop.testkit.channel.ChannelType;
import com.mycompany.myshop.systemtest.latest.e2e.base.BaseE2eTest;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;

class PlaceOrderPositiveTest extends BaseE2eTest {
    @Tag("UC-001.1")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldPlaceOrder() {
        scenario
                .when().placeOrder()
                .then().shouldSucceed();
    }
}



