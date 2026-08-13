package com.mycompany.myshop.backend.integration.contract.erp;

import com.mycompany.myshop.backend.core.services.external.ErpGateway;
import com.mycompany.myshop.backend.testkit.driver.adapter.external.erp.client.SimulatorErpProductClient;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The {@code Real} side of the ERP product contract: same two scenarios as
 * {@link ErpStubContractIntegrationTest}, run against the actual ERP simulator
 * ({@code external-systems/simulators/mock-server.js}) instead of a WireMock stand-in. Where the stub
 * side proves the production {@link ErpGateway} still agrees with our own guess about the ERP, this
 * side proves it still agrees with the real thing.
 *
 * <p>Deliberately named without an {@code IntegrationTest} suffix so it is excluded from
 * {@code component-tests.yaml}'s {@code integration} suite (which runs {@code --tests '*IntegrationTest'}
 * as part of the commit-stage) — this class needs the simulator running and is not part of that fast,
 * Docker-optional default. Wiring it into CI is tracked separately; see
 * {@code shop/plans/} for the deferred ERP-real-contract CI item.
 *
 * <p>Prerequisite: start the simulator with
 * {@code docker compose -f docker/java/multitier/docker-compose.local.real.yml up external-system-simulators}
 * (exposes it on {@code localhost:9111}, matching {@link #REAL_BASE_URL}'s default).
 */
class ErpRealContractTest extends BaseErpProductContractIntegrationTest {

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
