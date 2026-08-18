package com.mycompany.myshop.backend.contract.external.clock;

import com.mycompany.myshop.backend.BaseComponentTest;
import org.junit.jupiter.api.Test;

class ClockStubConsumabilityContractTest extends BaseComponentTest {

    @Test
    void stubTimeIsConsumableBySut() {
        scenario
            .given().clock().withTime("2026-01-15T10:30:00Z")
            .then().clock().hasTime("2026-01-15T10:30:00Z");
    }
}
