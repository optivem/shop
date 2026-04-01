package com.optivem.eshop.systemtest.legacy.mod07.e2e;

import com.optivem.eshop.dsl.channel.ChannelType;
import com.optivem.eshop.systemtest.legacy.mod07.e2e.base.BaseE2eTest;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.TestTemplate;

import static com.optivem.eshop.systemtest.commons.constants.Defaults.*;

class PlaceOrderNegativeTest extends BaseE2eTest {
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldRejectOrderWithNonIntegerQuantity() {
        app.shop().placeOrder().sku(SKU).quantity("3.5").execute()
                .shouldFail()
                .errorMessage("The request contains one or more validation errors")
                .fieldErrorMessage("quantity", "Quantity must be an integer");
    }
}
