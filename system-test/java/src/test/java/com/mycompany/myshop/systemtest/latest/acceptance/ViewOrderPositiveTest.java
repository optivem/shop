package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
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
    void customerShouldBeAbleToViewOwnOrder() {
        scenario
                .given().order()
                    .placedByCustomer()
                .and().loggedInAsCustomer()
                .when().viewOrder()
                .then().shouldSucceed();
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void sameCustomerAliasShouldResolveToSameCustomer() {
        scenario
                .given().order()
                    .placedByCustomer("B")
                .and().loggedInAsCustomer("B")
                .when().viewOrder()
                .then().shouldSucceed();
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void adminShouldBeAbleToViewCustomerOrder() {
        scenario
                .given().order()
                    .placedByCustomer()
                .and().loggedInAsAdmin()
                .when().viewOrder()
                .then().shouldSucceed();
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void adminShouldBeAbleToViewAnotherCustomersOrder() {
        scenario
                .given().order()
                    .placedByCustomer("B")
                .and().loggedInAsAdmin()
                .when().viewOrder()
                .then().shouldSucceed();
    }
}
