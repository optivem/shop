package com.optivem.eshop.systemtest.legacy.mod10.acceptance;

import com.optivem.eshop.systemtest.legacy.mod10.acceptance.base.BaseAcceptanceTest;
import com.optivem.eshop.dsl.channel.ChannelType;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.TestTemplate;

class PlaceOrderNegativeTest extends BaseAcceptanceTest {
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldRejectOrderWithNonIntegerQuantity() {
        scenario
                .when().placeOrder()
                    .withQuantity("3.5")
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("quantity", "Quantity must be an integer");
    }
}
