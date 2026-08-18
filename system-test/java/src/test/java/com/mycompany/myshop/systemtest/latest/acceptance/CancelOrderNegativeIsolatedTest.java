package com.mycompany.myshop.systemtest.latest.acceptance;

import com.mycompany.myshop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.mycompany.myshop.testkit.common.domain.OrderStatus;
import com.optivem.testing.*;
import org.junit.jupiter.api.TestTemplate;

@Isolated("mutates the shared wall clock into the Dec 31 cancellation blackout window; parallel runs would clash on the clock")
class CancelOrderNegativeIsolatedTest extends BaseAcceptanceTest {
    @TestTemplate
    @Channel(value = {ChannelType.API}, alsoForFirstRow = ChannelType.UI)
    @DataSource({"2024-12-31T22:00:00Z"})   // Start of blackout period
    @DataSource({"2026-12-31T22:00:01Z"})   // Just after start
    @DataSource({"2025-12-31T22:15:00Z"})   // Middle of blackout period
    @DataSource({"2028-12-31T22:29:59Z"})   // Just before end
    @DataSource({"2021-12-31T22:30:00Z"})   // End of blackout period
    void cannotCancelAnOrderOn31stDecBetween2200And2230(String timeIso) {
        scenario
                .given().clock()
                    .withTime(timeIso)
                .and().order()
                    .withStatus(OrderStatus.PLACED)
                .when().cancelOrder()
                .then().shouldFail()
                    .errorMessage("Order cancellation is not allowed on December 31st between 22:00 and 23:00")
                .and().order()
                    .hasStatus(OrderStatus.PLACED);
    }
}
