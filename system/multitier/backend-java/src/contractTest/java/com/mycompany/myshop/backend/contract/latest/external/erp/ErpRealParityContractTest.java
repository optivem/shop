package com.mycompany.myshop.backend.contract.latest.external.erp;

import com.mycompany.myshop.backend.core.services.external.ErpGateway;
import com.mycompany.myshop.backend.testkit.driver.adapter.external.erp.client.SimulatorErpProductClient;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The {@code Real} side of the ERP product contract: same two scenarios as
 * {@link ErpStubParityContractTest}, run against the actual ERP simulator
 * ({@code external-systems/simulators/mock-server.js}) instead of a WireMock stand-in. Where the stub
 * side proves the production {@link ErpGateway} still agrees with our own guess about the ERP, this
 * side proves it still agrees with the real thing.
 *
 * <p>Deliberately carries the {@code Real} role marker rather than {@code Stub}, so it is excluded
 * from {@code component-tests.yaml}'s {@code external-contract} suite — that suite selects the stub
 * side only ({@code --tests '*StubParityContractTest' --tests '*StubConsumabilityContractTest'}).
 * This class needs the simulator running and is not part of the commit-stage default. Wiring it into
 * CI is tracked separately; see
 * {@code shop/plans/20260812-1600-erp-real-contract-ci-wiring.md}.
 *
 * <p>Prerequisite: start the simulator with
 * {@code docker compose -f docker/java/multitier/docker-compose.local.real.yml up external-system-simulators}
 * (exposes it on {@code localhost:9111}, matching {@link #REAL_BASE_URL}'s default).
 */
class ErpRealParityContractTest extends BaseErpProductParityContractTest {

    private static final String REAL_BASE_URL =
        System.getenv().getOrDefault("ERP_REAL_BASE_URL", "http://localhost:9111/erp");

    private final SimulatorErpProductClient client = new SimulatorErpProductClient(REAL_BASE_URL);

    private ErpGateway erpGateway;

    @BeforeEach
    void setUp() {
        erpGateway = new ErpGateway();
        ReflectionTestUtils.setField(erpGateway, "erpUrl", REAL_BASE_URL);
    }

    @Override
    protected void arrangeProduct(String sku, String price) {
        client.createProduct(sku, price);
    }

    @Override
    protected ErpGateway erpGateway() {
        return erpGateway;
    }
}
