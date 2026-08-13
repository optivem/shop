package com.mycompany.myshop.backend.integration.contract.erp;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.core.services.external.ErpGateway;
import org.junit.jupiter.api.Test;

/**
 * Shared shape for the ERP product contract: does the production {@link ErpGateway}'s parse still
 * agree with whatever is answering at {@link #erpGateway()}'s URL? {@link ErpStubContractIntegrationTest}
 * answers that question against the hand-written WireMock stub (same question as
 * {@code legacy/ErpGatewayIntegrationTest}); {@link ErpRealContractTest} answers it against the real
 * ERP simulator. Both provision-and-pin the same exact values, so the assertions below don't need to
 * know which mode is running.
 *
 * <p>Scoped to products only: the simulator's promotion endpoint is hardcoded and its error responses
 * cannot be provoked on demand, so a real-mode twin for those isn't buildable — see
 * {@code shop/plans/20260717-1015-component-stub-contract-beyond.md} item 4.
 */
abstract class BaseErpProductContractIntegrationTest {

    protected abstract void arrangeProduct(String sku, String price);

    protected abstract ErpGateway erpGateway();

    @Test
    void getProductDetailsReturnsDetailsWhenFound() {
        arrangeProduct("BOOK-123", "10.00");

        var result = erpGateway().getProductDetails("BOOK-123");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("BOOK-123");
        assertThat(result.get().getPrice()).isEqualByComparingTo("10.00");
    }

    @Test
    void getProductDetailsReturnsEmptyWhenNotFound() {
        assertThat(erpGateway().getProductDetails("UNKNOWN-CONTRACT-SKU")).isEmpty();
    }
}
