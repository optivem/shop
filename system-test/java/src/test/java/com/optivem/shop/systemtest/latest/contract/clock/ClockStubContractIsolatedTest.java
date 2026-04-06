package com.optivem.shop.systemtest.latest.contract.clock;

import com.optivem.shop.systemtest.configuration.ExternalSystemMode;
import com.optivem.shop.systemtest.latest.contract.base.BaseExternalSystemContractTest;
import com.optivem.testing.Isolated;
import org.junit.jupiter.api.Test;

@Isolated
class ClockStubContractIsolatedTest extends BaseExternalSystemContractTest {
    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.STUB;
    }

    @Test
    void shouldBeAbleToGetConfiguredTime() {
        scenario
                .given().clock().withTime("2024-01-02T09:00:00Z")
                .then().clock().hasTime("2024-01-02T09:00:00Z");
    }
}
