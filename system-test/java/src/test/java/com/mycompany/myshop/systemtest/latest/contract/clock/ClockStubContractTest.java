package com.mycompany.myshop.systemtest.latest.contract.clock;

import com.mycompany.myshop.testkit.dsl.port.ExternalSystemMode;

class ClockStubContractTest extends BaseClockContractTest {
    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.STUB;
    }
}
