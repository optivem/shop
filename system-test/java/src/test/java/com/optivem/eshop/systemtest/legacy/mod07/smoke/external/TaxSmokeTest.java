package com.optivem.eshop.systemtest.legacy.mod07.smoke.external;

import com.optivem.eshop.systemtest.legacy.mod07.base.BaseUseCaseDslTest;
import org.junit.jupiter.api.Test;

class TaxSmokeTest extends BaseUseCaseDslTest {
    @Test
    void shouldBeAbleToGoToTax() {
        app.tax().goToTax()
                .execute()
                .shouldSucceed();
    }
}


