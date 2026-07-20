package com.mycompany.myshop.backend.component.latest.harness.external;

import com.mycompany.myshop.backend.AbstractComponentTest;
import org.junit.jupiter.api.Test;

/** Liveness canary for the in-process Clock stub. See {@link ErpSmokeTest} for what it does not cover. */
class ClockSmokeTest extends AbstractComponentTest {

    @Test
    void shouldBeAbleToGoToClock() {
        scenario.assume().clock().shouldBeRunning();
    }
}
