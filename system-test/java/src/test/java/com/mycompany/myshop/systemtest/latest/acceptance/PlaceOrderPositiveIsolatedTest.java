package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.optivem.testing.Channel;
import com.optivem.testing.Isolated;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;

@Isolated
class PlaceOrderPositiveIsolatedTest extends BaseAcceptanceTest {
    @Tag("UC-001.6")
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

    @Tag("UC-001.7")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldApplyFullPriceWithoutPromotion() {
        scenario
                .given().product().withUnitPrice(20.00)
                .and().promotion().withActive(false)
                .and().country().withTaxRate("0.00")
                .when().placeOrder().withQuantity(5)
                .then().shouldSucceed()
                .and().order()
                    .hasTotalPrice(100.00);
    }

    @Tag("UC-001.7")
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldApplyDiscountWhenPromotionIsActive() {
        scenario
                .given().product().withUnitPrice(20.00)
                .and().promotion().withActive(true).withDiscount(0.5)
                .and().country().withTaxRate("0.00")
                .when().placeOrder().withQuantity(5)
                .then().shouldSucceed()
                .and().order()
                    .hasTotalPrice(50.00);
    }
}
