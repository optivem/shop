package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.TestTemplate;

import static com.mycompany.myshop.systemtest.commons.constants.ErrorMessages.PERMISSION_DENIED;

class DeliverOrderNegativeTest extends BaseAcceptanceTest {
    @TestTemplate
    @Channel(ChannelType.API)
    void customerShouldNotBeAbleToDeliverOrder() {
        scenario
                .given().loggedInAsCustomer()
                .when().deliverOrder()
                    .withOrderNumber("ORD-NONEXISTENT")
                .then().shouldFail()
                    .errorMessage(PERMISSION_DENIED);
    }
}
