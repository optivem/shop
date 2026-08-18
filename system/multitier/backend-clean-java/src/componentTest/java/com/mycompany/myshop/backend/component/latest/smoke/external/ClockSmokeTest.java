package com.mycompany.myshop.backend.component.latest.smoke.external;

import com.mycompany.myshop.backend.BaseComponentTest;
import org.junit.jupiter.api.Test;

class ClockSmokeTest extends BaseComponentTest {

    @Test
    void shouldBeAbleToGoToClock() {
        scenario.assume().clock().shouldBeRunning();
    }
}
