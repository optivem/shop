package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.TestTemplate;

import static com.mycompany.myshop.systemtest.commons.constants.ErrorMessages.PERMISSION_DENIED;

class BrowseCouponsNegativeTest extends BaseAcceptanceTest {
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void customerShouldNotBeAbleToBrowseCoupons() {
        scenario
                .given().loggedInAsCustomer()
                .when().browseCoupons()
                .then().shouldFail()
                    .errorMessage(PERMISSION_DENIED);
    }

    @TestTemplate
    @Channel(ChannelType.API)
    void shouldRejectPageSizeAboveTheCap() {
        scenario
                .when().browseCoupons()
                    .withSize("101")
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("size", "Page size must be between 1 and 100");
    }

    @TestTemplate
    @Channel(ChannelType.API)
    void shouldRejectPageBelowTheFirst() {
        scenario
                .when().browseCoupons()
                    .withPage("0")
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("page", "Page must be 1 or greater");
    }
}
