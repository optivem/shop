package com.optivem.eshop.systemtest.acceptance;

import com.optivem.eshop.systemtest.acceptance.base.BaseAcceptanceTest;
import com.optivem.eshop.dsl.channel.ChannelType;
import com.optivem.testing.Channel;

import org.junit.jupiter.api.TestTemplate;

class PlaceOrderNegativeTest extends BaseAcceptanceTest {
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldRejectOrderWithInvalidQuantity() {
        scenario
                .when().placeOrder()
                    .withQuantity("invalid-quantity")
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("quantity", "Quantity must be an integer");
    }
}
