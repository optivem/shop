package com.mycompany.myshop.backend.component.latest.smoke.external;

import com.mycompany.myshop.backend.BaseComponentTest;
import org.junit.jupiter.api.Test;

class ErpSmokeTest extends BaseComponentTest {

    @Test
    void shouldBeAbleToGoToErp() {
        scenario.assume().erp().shouldBeRunning();
    }
}
