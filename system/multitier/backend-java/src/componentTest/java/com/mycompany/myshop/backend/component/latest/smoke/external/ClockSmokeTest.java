package com.mycompany.myshop.backend.component.latest.smoke.external;

import com.mycompany.myshop.backend.BaseComponentTest;
import org.junit.jupiter.api.Test;

/** Liveness canary for the in-process Clock stub. See {@link ErpSmokeTest} for what it does not cover. */
class ClockSmokeTest extends BaseComponentTest {

    @Test
    void shouldBeAbleToGoToClock() {
        scenario.assume().clock().shouldBeRunning();
    }
}
