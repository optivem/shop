package com.optivem.shop.systemtest.latest.contract.clock;

import com.optivem.shop.systemtest.latest.contract.base.BaseExternalSystemContractTest;
import org.junit.jupiter.api.Test;

public abstract class BaseClockContractTest extends BaseExternalSystemContractTest {
    @Test
    void shouldBeAbleToGetTime() {
        scenario
                .given().clock().withTime("2024-01-02T09:00:00Z")
                .then().clock().hasTime();
    }
}
