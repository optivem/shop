package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.systemtest.latest.acceptance.base.RequiresKeycloak;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.TestTemplate;

import static com.mycompany.myshop.systemtest.commons.constants.ErrorMessages.PERMISSION_DENIED;

class BrowseCouponsNegativeTest extends BaseAcceptanceTest {
    @TestTemplate
    @Channel(ChannelType.API)
    @RequiresKeycloak
    void customerShouldNotBeAbleToBrowseCoupons() {
        scenario
                .given().loggedInAsCustomer()
                .when().browseCoupons()
                .then().shouldFail()
                    .errorMessage(PERMISSION_DENIED);
    }
}
