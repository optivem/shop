package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;

import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.DEFAULT_ORDER_NUMBER;

class BrowseOrderHistoryPositiveTest extends BaseAcceptanceTest {
    @Tag("UC-002.1")
    @Tag("UC-002.2")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void customerShouldSeeOwnOrderInHistory() {
        scenario
                .given().order()
                    .placedByCustomer()
                .and().loggedInAsCustomer()
                .when().browseOrderHistory()
                .then().shouldSucceed()
                    .orderHistory()
                        .containsOrder(DEFAULT_ORDER_NUMBER);
    }

    @Tag("UC-011.1")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void adminShouldSeeCustomerOrderInHistory() {
        scenario
                .given().order()
                    .placedByCustomer()
                .and().loggedInAsAdmin()
                .when().browseOrderHistory()
                .then().shouldSucceed()
                    .orderHistory()
                        .containsOrder(DEFAULT_ORDER_NUMBER);
    }

    @Tag("UC-011.1")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void adminShouldSeeAnotherCustomersOrderInHistory() {
        scenario
                .given().order()
                    .placedByCustomer("B")
                .and().loggedInAsAdmin()
                .when().browseOrderHistory()
                .then().shouldSucceed()
                    .orderHistory()
                        .containsOrder(DEFAULT_ORDER_NUMBER);
    }

    @Tag("UC-002.3")
    @TestTemplate
    @Channel(ChannelType.API)
    void firstPageShouldReportTheTotalNumberOfMatchingOrders() {
        scenario
                .given().order()
                    .placedByCustomer()
                .and().loggedInAsCustomer()
                .when().browseOrderHistory()
                    .withPage("1")
                    .withSize("1")
                .then().shouldSucceed()
                    .orderHistory()
                        .containsOrder(DEFAULT_ORDER_NUMBER)
                        .hasTotalElements(1);
    }

    @Tag("UC-002.3")
    @TestTemplate
    @Channel(ChannelType.API)
    void pagePastTheLastShouldBeEmpty() {
        scenario
                .given().order()
                    .placedByCustomer()
                .and().loggedInAsCustomer()
                .when().browseOrderHistory()
                    .withPage("2")
                .then().shouldSucceed()
                    .orderHistory()
                        .isEmpty();
    }

    @Tag("UC-011.3")
    @Tag("UC-011.4")
    @TestTemplate
    @Channel(ChannelType.API)
    void adminShouldSeeOrderOfChosenCustomer() {
        scenario
                .given().order()
                    .withOrderNumber("ORD-A")
                    .placedByCustomer("A")
                .and().loggedInAsAdmin()
                .when().browseOrderHistory()
                    .withCustomer("A")
                    .withOrderNumber("ORD-A")
                .then().shouldSucceed()
                    .orderHistory()
                        .containsOrder("ORD-A");
    }

    @Tag("UC-011.5")
    @TestTemplate
    @Channel(ChannelType.API)
    void customerFilterShouldBeIgnoredForCustomer() {
        scenario
                .given().order()
                    .withOrderNumber("ORD-A")
                    .placedByCustomer("A")
                .and().order()
                    .withOrderNumber("ORD-B")
                    .placedByCustomer("B")
                .and().loggedInAsCustomer("A")
                .when().browseOrderHistory()
                    .withCustomer("B")
                    .withOrderNumber("ORD-A")
                .then().shouldSucceed()
                    .orderHistory()
                        .containsOrder("ORD-A");
    }
}
