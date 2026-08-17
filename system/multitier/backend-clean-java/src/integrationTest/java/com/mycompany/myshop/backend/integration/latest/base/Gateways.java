package com.mycompany.myshop.backend.integration.latest.base;

import com.mycompany.myshop.backend.backendtest.configuration.ExternalSystemMode;
import com.mycompany.myshop.backend.domain.gateways.ClockGateway;
import com.mycompany.myshop.backend.domain.gateways.ErpGateway;
import com.mycompany.myshop.backend.domain.gateways.TaxGateway;
import com.mycompany.myshop.backend.infrastructure.external.clock.HttpClockGateway;
import com.mycompany.myshop.backend.infrastructure.external.erp.HttpErpGateway;
import com.mycompany.myshop.backend.infrastructure.external.tax.HttpTaxGateway;

/**
 * Builds the SUT's production gateway adapters outside a Spring context.
 *
 * <p>A narrow-integration test drives one gateway directly — no {@code @SpringBootTest}, so nothing
 * resolves the {@code @Value} constructor arguments and the test passes them itself.
 *
 * <p>This is the one place in the layer that names the {@code Http*Gateway} adapters; everything
 * above it works against the domain ports they implement, which is why the return types below are
 * the interfaces rather than the concrete classes.
 *
 * <p>The component layer needs none of this — it boots Spring and autowires the real beans, pointing
 * them at the stubs through {@code @DynamicPropertySource}. Hence this class lives in
 * {@code integrationTest} rather than in the shared {@code testSupport} source set.
 */
final class Gateways {

    private Gateways() {
    }

    static ErpGateway erp(String baseUrl) {
        return new HttpErpGateway(baseUrl);
    }

    static TaxGateway tax(String baseUrl) {
        return new HttpTaxGateway(baseUrl);
    }

    static ClockGateway clock(String baseUrl, ExternalSystemMode mode) {
        return clockWithRawMode(baseUrl, mode.propertyValue());
    }

    /**
     * Escape hatch for the one case the enum cannot express: an {@code external.system-mode} value the
     * SUT does not recognise, which must reach {@link HttpClockGateway}'s unknown-mode branch.
     * Deliberately takes a raw string — typing this argument would make the branch untestable.
     */
    static ClockGateway clockWithRawMode(String baseUrl, String rawMode) {
        return new HttpClockGateway(rawMode, baseUrl);
    }
}
