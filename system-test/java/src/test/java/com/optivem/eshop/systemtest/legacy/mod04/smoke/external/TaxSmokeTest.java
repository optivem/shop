package com.optivem.eshop.systemtest.legacy.mod04.smoke.external;

import com.optivem.eshop.systemtest.legacy.mod04.base.BaseClientTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.optivem.eshop.dsl.common.ResultAssert.assertThatResult;

class TaxSmokeTest extends BaseClientTest {
    @BeforeEach
    void setUp() {
        setUpExternalClients();
    }

    @Test
    void shouldBeAbleToGoToTax() {
        var result = taxClient.checkHealth();
        assertThatResult(result).isSuccess();
    }
}


