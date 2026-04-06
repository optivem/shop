package com.optivem.shop.systemtest.latest.acceptance;

import com.optivem.shop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.optivem.shop.dsl.channel.ChannelType;
import com.optivem.testing.Channel;
import com.optivem.testing.Isolated;

import org.junit.jupiter.api.TestTemplate;

@Isolated
class PlaceOrderPositiveIsolatedTest extends BaseAcceptanceTest {
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldRecordPlacementTimestamp() {
        scenario
                .given().clock()
                    .withTime("2026-01-15T10:30:00Z")
                .when().placeOrder()
                .then().shouldSucceed()
                .and().clock()
                    .hasTime("2026-01-15T10:30:00Z");
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldApplyFullPriceWithoutPromotion() {
        scenario
                .given().product().withUnitPrice(20.00)
                .and().promotion().withActive(false)
                .and().country().withCode("US").withTaxRate("0.00")
                .when().placeOrder().withQuantity(5)
                .then().shouldSucceed()
                .and().order()
                    .hasTotalPrice(100.00);
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldApplyDiscountWhenPromotionIsActive() {
        scenario
                .given().product().withUnitPrice(20.00)
                .and().promotion().withActive(true).withDiscount(0.5)
                .and().country().withCode("US").withTaxRate("0.00")
                .when().placeOrder().withQuantity(5)
                .then().shouldSucceed()
                .and().order()
                    .hasTotalPrice(50.00);
    }
}
