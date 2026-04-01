package com.optivem.eshop.systemtest.legacy.mod10.acceptance;

import com.optivem.eshop.systemtest.legacy.mod10.acceptance.base.BaseAcceptanceTest;
import com.optivem.eshop.dsl.channel.ChannelType;
import com.optivem.eshop.dsl.driver.port.shop.dtos.OrderStatus;
import com.optivem.testing.*;
import org.junit.jupiter.api.TestTemplate;

@Isolated
class CancelOrderNegativeIsolatedTest extends BaseAcceptanceTest {
    @TimeDependent
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
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


