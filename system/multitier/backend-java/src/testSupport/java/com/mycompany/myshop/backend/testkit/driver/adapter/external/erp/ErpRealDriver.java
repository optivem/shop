package com.mycompany.myshop.backend.testkit.driver.adapter.external.erp;

import com.mycompany.myshop.backend.testkit.driver.adapter.external.erp.client.ErpRealClient;
import com.mycompany.myshop.backend.testkit.driver.port.external.erp.ErpDriver;

/**
 * ERP driver backed by the real simulator via {@link ErpRealClient}, mirroring system-test's own
 * {@code ErpRealDriver}. {@code returnsProduct} provisions the fixture for real, since there is nothing
 * to stub. {@code returnsNoProduct}/{@code returnsPromotion} are no-ops: a SKU that was never created is
 * already absent, and the simulator's promotion endpoint is hardcoded, so there is nothing to arrange —
 * same shape as system-test's {@code ErpRealDriver.returnsPromotion}. {@code failsForProduct}/
 * {@code failsForPromotion} have no real counterpart at all — the simulator cannot be told to fail on
 * demand — so they throw rather than silently succeed, which would make an error-path test pass for the
 * wrong reason.
 */
public class ErpRealDriver implements ErpDriver {

    private final ErpRealClient client;

    public ErpRealDriver(String baseUrl) {
        this.client = new ErpRealClient(baseUrl);
    }

    @Override
    public void goToErp() {
        client.checkHealth();
    }

    @Override
    public void returnsProduct(String sku, String price) {
        client.createProduct(sku, price);
    }

    @Override
    public void returnsNoProduct(String sku) {
        // no-op: a SKU that was never created is already absent
    }

    @Override
    public void returnsPromotion(boolean active, String discount) {
        // no-op: the simulator's promotion endpoint is hardcoded, nothing to arrange
    }

    @Override
    public void failsForProduct(String sku, int status, String body) {
        throw new UnsupportedOperationException(
            "ErpRealDriver cannot force a failure response — the real ERP simulator has no fault-injection endpoint.");
    }

    @Override
    public void failsForPromotion(int status, String body) {
        throw new UnsupportedOperationException(
            "ErpRealDriver cannot force a failure response — the real ERP simulator has no fault-injection endpoint.");
    }
}
