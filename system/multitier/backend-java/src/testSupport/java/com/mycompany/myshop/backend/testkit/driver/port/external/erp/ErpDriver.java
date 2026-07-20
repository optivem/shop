package com.mycompany.myshop.backend.testkit.driver.port.external.erp;

/**
 * What the use case DSL needs from the ERP: the ability to program its responses. Mirrors the
 * system-test project's {@code testkit/driver/port/external/erp/ErpDriver}, so {@code ErpDsl} depends
 * on a port rather than on a concrete WireMock-backed adapter.
 *
 * <p>The vocabulary here is stub-programming only, unlike the system-test port which also reads back
 * from the ERP. At these layers reading the ERP is the SUT's job — see
 * {@code driver/adapter/sut/SutErpReader}, which reads through the SUT's production gateway.
 */
public interface ErpDriver {

    /**
     * Liveness probe behind {@code assume().erp().shouldBeRunning()}: throws if the stub server this
     * driver is pointed at cannot be reached. Proves the driver's own client resolves to a live
     * WireMock — not that it resolves to the <em>right</em> one, which is the stub-contract tests'
     * job (they plant through this driver and read back through the SUT's production gateway).
     */
    void goToErp();

    void stubProduct(String sku, String price);

    void stubProductMissing(String sku);

    void stubPromotion(boolean active, String discount);

    void stubProductError(String sku, int status, String body);

    void stubPromotionError(int status, String body);
}
