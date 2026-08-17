package com.mycompany.myshop.backend.contract.external.clock;

import com.mycompany.myshop.backend.BaseComponentTest;
import org.junit.jupiter.api.Test;

/**
 * Pins that the component harness's Clock WireMock stub is CONSUMABLE BY THE SUT. The read-back goes
 * through the SUT's production {@code HttpClockGateway} (real HTTP + real {@code GetTimeResponse} parse),
 * so a field drift in {@code ClockStubDriver} fails this test rather than silently mis-reading the
 * time. See {@code ErpStubConsumabilityContractTest} for the full rationale.
 */
class ClockStubConsumabilityContractTest extends BaseComponentTest {

    @Test
    void stubTimeIsConsumableBySut() {
        scenario
            .given().clock().withTime("2026-01-15T10:30:00Z")
            .then().clock().hasTime("2026-01-15T10:30:00Z");
    }
}
