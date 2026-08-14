package com.mycompany.myshop.backend.component.latest.smoke.external;

import com.mycompany.myshop.backend.BaseComponentTest;
import org.junit.jupiter.api.Test;

/**
 * Liveness canary for the in-process ERP stub, mirroring system-test's {@code
 * latest/smoke/external/ErpSmokeTest} so the layers read alike.
 *
 * <p>Narrower than its system-test twin: there ERP is a compose container the suite does not own, so
 * the probe is a genuine pre-flight. Here the stub is started by {@code BaseComponentTest}'s
 * static initializer, so this fails only if the driver's client cannot reach a live WireMock. The
 * load-bearing wiring check is {@code contract/ErpStubConsumabilityContractTest}, which plants through
 * this driver and reads back through the SUT's production gateway.
 */
class ErpSmokeTest extends BaseComponentTest {

    @Test
    void shouldBeAbleToGoToErp() {
        scenario.assume().erp().shouldBeRunning();
    }
}
