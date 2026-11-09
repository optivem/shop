package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;

import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.DEFAULT_ORDER_NUMBER;

class BrowseOrderHistoryNegativeTest extends BaseAcceptanceTest {
    @Tag("UC-002.6")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void customerShouldNotSeeAnotherCustomersOrderInHistory() {
        scenario
                .given().order()
                    .placedByCustomer("B")
                .and().loggedInAsCustomer("A")
                .when().browseOrderHistory()
                .then().shouldSucceed()
                    .orderHistory()
                        .doesNotContainOrder(DEFAULT_ORDER_NUMBER);
    }

    @Tag("UC-002.8")
    @TestTemplate
    @Channel(ChannelType.API)
    void shouldRejectPageSizeAboveTheCap() {
        scenario
                .when().browseOrderHistory()
                    .withSize("101")
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("size", "Page size must be between 1 and 100");
    }

    @Tag("UC-002.8")
    @TestTemplate
    @Channel(ChannelType.API)
    void shouldRejectPageBelowTheFirst() {
        scenario
                .when().browseOrderHistory()
                    .withPage("0")
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("page", "Page must be 1 or greater");
    }

    @Tag("UC-002.8")
    @TestTemplate
    @Channel(ChannelType.API)
    void shouldRejectPageSizeBelowOne() {
        scenario
                .when().browseOrderHistory()
                    .withSize("0")
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("size", "Page size must be between 1 and 100");
    }

    @Tag("UC-002.9")
    @TestTemplate
    @Channel(ChannelType.API)
    void shouldShowNoOrdersWhenNoOrderMatchesTheFilter() {
        scenario
                .given().loggedInAsCustomer()
                .when().browseOrderHistory()
                    .withOrderNumber("NON-EXISTENT-ORDER-99999")
                .then().shouldSucceed()
                    .orderHistory()
                        .isEmpty();
    }
}
