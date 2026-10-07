package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.TestTemplate;

import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.DEFAULT_ORDER_NUMBER;

class BrowseOrderHistoryNegativeTest extends BaseAcceptanceTest {
    @TestTemplate
    @Channel(ChannelType.API)
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
}
