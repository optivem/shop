package com.mycompany.myshop.systemtest.legacy.mod11.contract.clock;

import com.mycompany.myshop.testkit.dsl.port.ExternalSystemMode;

class ClockRealContractTest extends BaseClockContractTest {
    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.REAL;
    }
}



