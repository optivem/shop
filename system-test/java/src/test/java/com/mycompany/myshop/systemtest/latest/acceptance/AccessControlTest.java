package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseIdentityAcceptanceTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.TestTemplate;

class AccessControlTest extends BaseIdentityAcceptanceTest {
    @TestTemplate
    @Channel(ChannelType.API)
    void healthShouldNotRequireAuthentication() {
        scenario.assume().actingAsAnonymous().myShop().shouldBeRunning();
    }

    @TestTemplate
    @Channel(ChannelType.API)
    void anonymousShouldNotBeAbleToViewOrder() {
        scenario
                .when().actingAsAnonymous().viewOrder()
                    .withOrderNumber("ORD-NONEXISTENT")
                .then().shouldFail()
                    .statusCode(401);
    }

    @TestTemplate
    @Channel(ChannelType.API)
    void customerShouldNotBeAbleToBrowseCoupons() {
        scenario
                .when().actingAsCustomer().browseCoupons()
                .then().shouldFail()
                    .statusCode(403);
    }

    @TestTemplate
    @Channel(ChannelType.API)
    void customerShouldNotBeAbleToPublishCoupon() {
        scenario
                .when().actingAsCustomer().publishCoupon()
                .then().shouldFail()
                    .statusCode(403);
    }

    @TestTemplate
    @Channel(ChannelType.API)
    void customerShouldNotBeAbleToDeliverOrder() {
        scenario
                .when().actingAsCustomer().deliverOrder()
                    .withOrderNumber("ORD-NONEXISTENT")
                .then().shouldFail()
                    .statusCode(403);
    }

    @TestTemplate
    @Channel(ChannelType.API)
    void adminShouldBeAbleToBrowseCoupons() {
        scenario
                .when().actingAsAdmin().browseCoupons()
                .then().shouldSucceed();
    }

    @TestTemplate
    @Channel(ChannelType.API)
    void adminShouldBeAbleToPublishCoupon() {
        scenario
                .when().actingAsAdmin().publishCoupon()
                .then().shouldSucceed();
    }

    @TestTemplate
    @Channel(ChannelType.API)
    void adminShouldNotBeAbleToPlaceOrder() {
        scenario
                .when().actingAsAdmin().placeOrder()
                .then().shouldFail()
                    .statusCode(403);
    }
}
