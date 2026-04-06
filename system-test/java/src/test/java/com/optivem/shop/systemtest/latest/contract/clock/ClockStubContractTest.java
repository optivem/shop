package com.optivem.shop.systemtest.latest.contract.clock;

import com.optivem.shop.systemtest.configuration.ExternalSystemMode;

class ClockStubContractTest extends BaseClockContractTest {
    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.STUB;
    }
}
