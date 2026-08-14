package com.mycompany.myshop.backend.integration.latest.base;

import com.mycompany.myshop.backend.core.services.external.ClockGateway;
import com.mycompany.myshop.backend.core.services.external.ErpGateway;
import com.mycompany.myshop.backend.core.services.external.TaxGateway;
import com.mycompany.myshop.backend.backendtest.configuration.ExternalSystemMode;

/**
 * Builds the SUT's production gateways outside a Spring context.
 *
 * <p>A narrow-integration test drives one gateway directly — no {@code @SpringBootTest}, so nothing
 * resolves the {@code @Value} constructor arguments and the test passes them itself.
 *
 * <p>The component layer needs none of this — it boots Spring and autowires the real beans, pointing
 * them at the stubs through {@code @DynamicPropertySource}. Hence this class lives in
 * {@code integrationTest} rather than in the shared {@code testSupport} source set.
 */
final class Gateways {

    private Gateways() {
    }

    static ErpGateway erp(String baseUrl) {
        return new ErpGateway(baseUrl);
    }

    static TaxGateway tax(String baseUrl) {
        return new TaxGateway(baseUrl);
    }

    static ClockGateway clock(String baseUrl, ExternalSystemMode mode) {
        return clockWithRawMode(baseUrl, mode.propertyValue());
    }

    /**
     * Escape hatch for the one case the enum cannot express: an {@code external.system-mode} value the
     * SUT does not recognise, which must reach {@link ClockGateway}'s unknown-mode branch. Deliberately
     * takes a raw string — typing this argument would make the branch untestable.
     */
    static ClockGateway clockWithRawMode(String baseUrl, String rawMode) {
        return new ClockGateway(rawMode, baseUrl);
    }
}
