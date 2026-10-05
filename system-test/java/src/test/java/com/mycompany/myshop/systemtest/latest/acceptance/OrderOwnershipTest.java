package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseIdentityAcceptanceTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.TestTemplate;

class OrderOwnershipTest extends BaseIdentityAcceptanceTest {
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void customerShouldBeAbleToViewOwnOrder() {
        scenario
                .given().order()
                    .placedByCustomer()
                .when().actingAsCustomer().viewOrder()
                .then().shouldSucceed();
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void customerShouldNotBeAbleToViewAnotherCustomersOrder() {
        scenario
                .given().order()
                    .placedByAnotherCustomer()
                .when().actingAsCustomer().viewOrder()
                .then().shouldFail()
                    .errorMessage("Order DEFAULT-ORDER does not exist.");
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void adminShouldBeAbleToViewCustomerOrder() {
        scenario
                .given().order()
                    .placedByCustomer()
                .when().actingAsAdmin().viewOrder()
                .then().shouldSucceed();
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void adminShouldBeAbleToViewAnotherCustomersOrder() {
        scenario
                .given().order()
                    .placedByAnotherCustomer()
                .when().actingAsAdmin().viewOrder()
                .then().shouldSucceed();
    }

    @TestTemplate
    @Channel(ChannelType.API)
    void customerShouldSeeOwnOrderInHistory() {
        scenario
                .given().order()
                    .placedByCustomer()
                .when().actingAsCustomer().browseOrderHistory()
                .then().shouldSucceed()
                    .orderHistory()
                        .containsOrder("DEFAULT-ORDER");
    }

    @TestTemplate
    @Channel(ChannelType.API)
    void customerShouldNotSeeAnotherCustomersOrderInHistory() {
        scenario
                .given().order()
                    .placedByAnotherCustomer()
                .when().actingAsCustomer().browseOrderHistory()
                .then().shouldSucceed()
                    .orderHistory()
                        .doesNotContainOrder("DEFAULT-ORDER");
    }

    @TestTemplate
    @Channel(ChannelType.API)
    void adminShouldSeeCustomerOrderInHistory() {
        scenario
                .given().order()
                    .placedByCustomer()
                .when().actingAsAdmin().browseOrderHistory()
                .then().shouldSucceed()
                    .orderHistory()
                        .containsOrder("DEFAULT-ORDER");
    }

    @TestTemplate
    @Channel(ChannelType.API)
    void adminShouldSeeAnotherCustomersOrderInHistory() {
        scenario
                .given().order()
                    .placedByAnotherCustomer()
                .when().actingAsAdmin().browseOrderHistory()
                .then().shouldSucceed()
                    .orderHistory()
                        .containsOrder("DEFAULT-ORDER");
    }
}
