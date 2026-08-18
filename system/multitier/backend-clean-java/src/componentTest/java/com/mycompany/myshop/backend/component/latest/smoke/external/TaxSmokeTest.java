package com.mycompany.myshop.backend.component.latest.smoke.external;

import com.mycompany.myshop.backend.BaseComponentTest;
import org.junit.jupiter.api.Test;

class TaxSmokeTest extends BaseComponentTest {

    @Test
    void shouldBeAbleToGoToTax() {
        scenario.assume().tax().shouldBeRunning();
    }
}
