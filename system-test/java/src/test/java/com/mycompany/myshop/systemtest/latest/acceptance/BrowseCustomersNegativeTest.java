package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;

import static com.mycompany.myshop.systemtest.commons.constants.ErrorMessages.AUTHENTICATION_REQUIRED;
import static com.mycompany.myshop.systemtest.commons.constants.ErrorMessages.PERMISSION_DENIED;

// API only: the UI has no customer list of its own (see BrowseCustomersPositiveTest).
class BrowseCustomersNegativeTest extends BaseAcceptanceTest {
    @Tag("UC-011.5")
    @TestTemplate
    @Channel(ChannelType.API)
    void customerShouldNotBeAbleToBrowseCustomers() {
        scenario
                .given().loggedInAsCustomer()
                .when().browseCustomers()
                .then().shouldFail()
                    .errorMessage(PERMISSION_DENIED);
    }

    @Tag("UC-011.6")
    @TestTemplate
    @Channel(ChannelType.API)
    void anonymousShouldNotBeAbleToBrowseCustomers() {
        scenario
                .given().notLoggedIn()
                .when().browseCustomers()
                .then().shouldFail()
                    .errorMessage(AUTHENTICATION_REQUIRED);
    }
}
