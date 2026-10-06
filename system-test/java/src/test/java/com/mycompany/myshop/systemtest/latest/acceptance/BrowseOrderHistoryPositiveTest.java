package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.systemtest.latest.acceptance.base.RequiresKeycloak;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.TestTemplate;

import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.DEFAULT_ORDER_NUMBER;

class BrowseOrderHistoryPositiveTest extends BaseAcceptanceTest {
    @TestTemplate
    @Channel(ChannelType.API)
    @RequiresKeycloak
    void customerShouldSeeOwnOrderInHistory() {
        scenario
                .given().order()
                    .placedByCustomer()
                .when().actingAsCustomer().browseOrderHistory()
                .then().shouldSucceed()
                    .orderHistory()
                        .containsOrder(DEFAULT_ORDER_NUMBER);
    }

    @TestTemplate
    @Channel(ChannelType.API)
    @RequiresKeycloak
    void adminShouldSeeCustomerOrderInHistory() {
        scenario
                .given().order()
                    .placedByCustomer()
                .when().actingAsAdmin().browseOrderHistory()
                .then().shouldSucceed()
                    .orderHistory()
                        .containsOrder(DEFAULT_ORDER_NUMBER);
    }

    @TestTemplate
    @Channel(ChannelType.API)
    @RequiresKeycloak
    void adminShouldSeeAnotherCustomersOrderInHistory() {
        scenario
                .given().order()
                    .placedByAnotherCustomer()
                .when().actingAsAdmin().browseOrderHistory()
                .then().shouldSucceed()
                    .orderHistory()
                        .containsOrder(DEFAULT_ORDER_NUMBER);
    }
}
