package com.mycompany.myshop.systemtest.legacy.mod11.contract.clock;

import com.mycompany.myshop.testkit.dsl.port.ExternalSystemMode;
import com.mycompany.myshop.systemtest.legacy.mod11.contract.base.BaseExternalSystemContractTest;
import com.optivem.testing.Isolated;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Isolated
class ClockStubContractIsolatedTest extends BaseExternalSystemContractTest {
    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.STUB;
    }

    @Test
    void shouldBeAbleToGetConfiguredTime() {
        assertDoesNotThrow(() -> scenario
                .given().clock().withTime("2024-01-02T09:00:00Z")
                .then().clock().hasTime("2024-01-02T09:00:00Z"));
    }
}
