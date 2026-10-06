package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.systemtest.latest.acceptance.base.RequiresKeycloak;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.TestTemplate;

class ViewOrderPositiveTest extends BaseAcceptanceTest {
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldBeAbleToViewOrder() {
        scenario
                .given().order()
                .when().viewOrder()
                .then().shouldSucceed();
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    @RequiresKeycloak
    void customerShouldBeAbleToViewOwnOrder() {
        scenario
                .given().order()
                    .placedByCustomer()
                .when().actingAsCustomer().viewOrder()
                .then().shouldSucceed();
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    @RequiresKeycloak
    void adminShouldBeAbleToViewCustomerOrder() {
        scenario
                .given().order()
                    .placedByCustomer()
                .when().actingAsAdmin().viewOrder()
                .then().shouldSucceed();
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    @RequiresKeycloak
    void adminShouldBeAbleToViewAnotherCustomersOrder() {
        scenario
                .given().order()
                    .placedByAnotherCustomer()
                .when().actingAsAdmin().viewOrder()
                .then().shouldSucceed();
    }
}
