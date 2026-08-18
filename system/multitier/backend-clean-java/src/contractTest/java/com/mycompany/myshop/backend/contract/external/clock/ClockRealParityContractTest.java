package com.mycompany.myshop.backend.contract.external.clock;

import com.mycompany.myshop.backend.backendtest.configuration.ExternalSystemMode;
import com.mycompany.myshop.backend.contract.external.ExternalSystemSimulator;
import com.mycompany.myshop.backend.domain.gateways.ClockGateway;
import com.mycompany.myshop.backend.infrastructure.external.clock.HttpClockGateway;

class ClockRealParityContractTest extends BaseClockTimeParityContractTest {

    private static final String BASE_URL = ExternalSystemSimulator.baseUrl("/clock");

    private final ClockGateway clockGateway =
            new HttpClockGateway(ExternalSystemMode.STUB.propertyValue(), BASE_URL);

    @Override
    protected ClockGateway clockGateway() {
        return clockGateway;
    }
}
