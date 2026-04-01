package com.optivem.eshop.systemtest.legacy.mod10.acceptance;

import com.optivem.eshop.systemtest.legacy.mod10.acceptance.base.BaseAcceptanceTest;
import com.optivem.eshop.dsl.channel.ChannelType;
import com.optivem.testing.Channel;
import com.optivem.testing.Isolated;

import org.junit.jupiter.api.TestTemplate;

@Isolated
class PlaceOrderPositiveIsolatedTest extends BaseAcceptanceTest {
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldApplyFullPriceOnWeekday() {
        scenario
                .given().product().withUnitPrice(20.00)
                .and().clock().withWeekday()
                .when().placeOrder().withQuantity(5)
                .then().shouldSucceed()
                .and().order()
                    .hasTotalPrice(100.00);
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldApplyWeekendDiscount() {
        scenario
                .given().product().withUnitPrice(20.00)
                .and().clock().withWeekend()
                .when().placeOrder().withQuantity(5)
                .then().shouldSucceed()
                .and().order()
                    .hasTotalPrice(50.00);
    }

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
}
