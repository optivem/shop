package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.systemtest.latest.acceptance.base.RequiresKeycloak;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.TestTemplate;

class DeliverOrderNegativeTest extends BaseAcceptanceTest {
    @TestTemplate
    @Channel(ChannelType.API)
    @RequiresKeycloak
    void customerShouldNotBeAbleToDeliverOrder() {
        scenario
                .when().actingAsCustomer().deliverOrder()
                    .withOrderNumber("ORD-NONEXISTENT")
                .then().shouldFail()
                    .statusCode(403);
    }
}
