package com.optivem.shop.systemtest.latest.acceptance;

import com.optivem.shop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.optivem.shop.dsl.channel.ChannelType;
import com.optivem.shop.dsl.driver.port.shop.dtos.OrderStatus;
import com.optivem.testing.Channel;

import org.junit.jupiter.api.TestTemplate;

class PlaceOrderPositiveTest extends BaseAcceptanceTest {
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void orderNumberShouldStartWithORD() {
        scenario
                .when().placeOrder()
                .then().shouldSucceed()
                .and().order()
                    .hasOrderNumberPrefix("ORD-");
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void orderStatusShouldBePlacedAfterPlacingOrder() {
        scenario
                .when().placeOrder()
                .then().shouldSucceed()
                .and().order()
                    .hasStatus(OrderStatus.PLACED);
    }

    @TestTemplate
    @Channel(ChannelType.API)
    void orderTotalShouldIncludeTax() {
        scenario
                .when().placeOrder()
                    .withCountry("DE")
                .then().shouldSucceed()
                .and().order()
                    .hasSubtotalPrice(20.00)
                    .hasTaxRate(0.19)
                    .hasTotalPrice(23.80);
    }

    @TestTemplate
    @Channel(ChannelType.API)
    void orderTotalShouldReflectCouponDiscount() {
        scenario
                .given().coupon()
                    .withCode("DISC10")
                    .withDiscountRate(0.10)
                .when().placeOrder()
                    .withCouponCode("DISC10")
                .then().shouldSucceed()
                .and().order()
                    .hasSubtotalPrice(18.00)
                    .hasDiscountRate(0.10)
                    .hasAppliedCouponCode("DISC10")
                    .hasTotalPrice(18.00);
    }

    @TestTemplate
    @Channel(ChannelType.API)
    void orderTotalShouldApplyCouponDiscountAndTax() {
        scenario
                .given().coupon()
                    .withCode("COMBO10")
                    .withDiscountRate(0.10)
                .when().placeOrder()
                    .withCountry("GB")
                    .withCouponCode("COMBO10")
                .then().shouldSucceed()
                .and().order()
                    .hasSubtotalPrice(18.00)
                    .hasDiscountRate(0.10)
                    .hasTaxRate(0.20)
                    .hasAppliedCouponCode("COMBO10")
                    .hasTotalPrice(21.60);
    }
}
