package com.mycompany.myshop.backend.component.latest.harness.external;

import com.mycompany.myshop.backend.AbstractComponentTest;
import org.junit.jupiter.api.Test;

/** Liveness canary for the in-process Tax stub. See {@link ErpSmokeTest} for what it does not cover. */
class TaxSmokeTest extends AbstractComponentTest {

    @Test
    void shouldBeAbleToGoToTax() {
        scenario.assume().tax().shouldBeRunning();
    }
}
