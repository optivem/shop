package com.optivem.shop.systemtest.latest.acceptance;

import com.optivem.shop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.optivem.shop.dsl.channel.ChannelType;
import com.optivem.shop.dsl.driver.port.shop.dtos.OrderStatus;
import com.optivem.testing.*;
import org.junit.jupiter.api.TestTemplate;

@Isolated
class CancelOrderPositiveIsolatedTest extends BaseAcceptanceTest {
    @TimeDependent
    @TestTemplate
    @Channel(value = {ChannelType.API}, alsoForFirstRow = ChannelType.UI)
    @DataSource({"2024-12-31T21:59:59Z"})   // 1 second before blackout period starts
    @DataSource({"2024-12-31T22:30:01Z"})   // 1 second after blackout period ends
    @DataSource({"2024-12-31T10:00:00Z"})   // Another time on blackout day but outside blackout period
    @DataSource({"2025-01-01T22:15:00Z"})   // Another day entirely (same time but different day)
    void shouldBeAbleToCancelOrderOutsideOfBlackoutPeriod31stDecBetween2200And2230(String timeIso) {
        scenario
                .given().clock()
                    .withTime(timeIso)
                .and().order()
                    .withStatus(OrderStatus.PLACED)
                .when().cancelOrder()
                .then().shouldSucceed();
    }
}
