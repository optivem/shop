package com.mycompany.myshop.backend.contract.external.erp;

import com.mycompany.myshop.backend.contract.external.ExternalSystemReal;
import com.mycompany.myshop.backend.core.services.external.ErpGateway;
import com.mycompany.myshop.backend.testkit.driver.adapter.external.erp.client.ErpRealClient;

/**
 * The {@code Real} side of the ERP product contract: same two scenarios as
 * {@link ErpStubParityContractTest}, run against the actual ERP simulator
 * ({@code external-systems/simulators/mock-server.js}) instead of a WireMock stand-in. Where the stub
 * side proves the production {@link ErpGateway} still agrees with our own guess about the ERP, this
 * side proves it still agrees with the real thing.
 *
 * <p>This is what makes the stub trustworthy enough to assert against — and the suite that asserts
 * against that stub ({@code component}) is a commit-stage suite, so this check runs in commit stage
 * too, as {@code component-tests.yaml}'s {@code external-contract-real}. A red run here beside a
 * green component run means the stub is lying and every component test leaning on it is currently
 * proving nothing; the fix is the pair, stub and component expectations together.
 *
 * <p>The simulator comes from {@link ExternalSystemReal} — see there for how it is started and
 * how to point this at an already-running instance instead.
 */
class ErpRealParityContractTest extends BaseErpProductParityContractTest {

    private static final String BASE_URL = ExternalSystemReal.baseUrl("/erp");

    private final ErpRealClient client = new ErpRealClient(BASE_URL);

    private final ErpGateway erpGateway = new ErpGateway(BASE_URL);

    @Override
    protected void arrangeProduct(String sku, String price) {
        client.createProduct(sku, price);
    }

    @Override
    protected ErpGateway erpGateway() {
        return erpGateway;
    }
}
