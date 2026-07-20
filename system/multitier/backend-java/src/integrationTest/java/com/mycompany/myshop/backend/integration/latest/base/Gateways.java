package com.mycompany.myshop.backend.integration.latest.base;

import com.mycompany.myshop.backend.core.services.external.ClockGateway;
import com.mycompany.myshop.backend.core.services.external.ErpGateway;
import com.mycompany.myshop.backend.core.services.external.TaxGateway;
import com.mycompany.myshop.backend.support.harness.ExternalSystemMode;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Builds the SUT's production gateways outside a Spring context.
 *
 * <p>A narrow-integration test drives one gateway directly — no {@code @SpringBootTest}, so nothing
 * injects the {@code @Value} fields and they have to be set reflectively. That leaks the gateways'
 * private field names into the tests, which is why it happens here and not in four separate files: a
 * rename of {@code erpUrl} now breaks one call site instead of going unnoticed until a test fails at
 * runtime with a null URL.
 *
 * <p>The component layer needs none of this — it boots Spring and autowires the real beans, pointing
 * them at the stubs through {@code @DynamicPropertySource}. Hence this class lives in
 * {@code integrationTest} rather than in the shared {@code testSupport} source set.
 */
final class Gateways {

    private Gateways() {
    }

    static ErpGateway erp(String baseUrl) {
        var gateway = new ErpGateway();
        ReflectionTestUtils.setField(gateway, "erpUrl", baseUrl);
        return gateway;
    }

    static TaxGateway tax(String baseUrl) {
        var gateway = new TaxGateway();
        ReflectionTestUtils.setField(gateway, "taxUrl", baseUrl);
        return gateway;
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
        var gateway = new ClockGateway();
        ReflectionTestUtils.setField(gateway, "clockUrl", baseUrl);
        ReflectionTestUtils.setField(gateway, "externalSystemMode", rawMode);
        return gateway;
    }
}
