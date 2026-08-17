package com.mycompany.myshop.backend.contract.external.clock;

import com.mycompany.myshop.backend.backendtest.configuration.ExternalSystemMode;
import com.mycompany.myshop.backend.contract.external.ExternalSystemReal;
import com.mycompany.myshop.backend.core.services.external.ClockGateway;

/**
 * The {@code Real} side of the clock time contract: the same scenario as
 * {@link ClockStubParityContractTest}, run against the actual clock simulator instead of a WireMock
 * stand-in. Closes the gap where {@code ClockStubDriver} was consumability-checked (its JSON parses
 * through the production gateway) but never held against the real system.
 *
 * <p>Runs the gateway in {@link ExternalSystemMode#STUB}, which is the mode that makes it fetch over
 * HTTP at all — in {@code REAL} mode {@code ClockGateway} short-circuits to {@code Instant.now()} and
 * never calls the clock service, so there would be no wire format to pin.
 *
 * <p>The simulator comes from {@link ExternalSystemReal} — see there for how it is started and
 * how to point this at an already-running instance instead.
 */
class ClockRealParityContractTest extends BaseClockTimeParityContractTest {

    private static final String BASE_URL = ExternalSystemReal.baseUrl("/clock");

    private final ClockGateway clockGateway =
            new ClockGateway(ExternalSystemMode.STUB.propertyValue(), BASE_URL);

    @Override
    protected ClockGateway clockGateway() {
        return clockGateway;
    }
}
