package com.mycompany.myshop.backend.contract.external.erp;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.domain.gateways.ErpGateway;
import com.mycompany.myshop.backend.domain.values.Money;
import org.junit.jupiter.api.Test;

/**
 * Shared shape for the ERP product contract: does the production {@link ErpGateway} adapter's parse
 * still agree with whatever is answering at {@link #erpGateway()}'s URL?
 * {@link ErpStubParityContractTest} answers that question against the hand-written WireMock stub
 * (same question as {@code legacy/ErpGatewayIntegrationTest}); {@link ErpRealParityContractTest}
 * answers it against the real ERP simulator. Both provision-and-pin the same exact values, so the
 * assertions below don't need to know which mode is running.
 *
 * <p>Typed against the domain port rather than the {@code HttpErpGateway} adapter that implements
 * it, so what is pinned is the whole job the adapter does: the HTTP call, the Jackson parse of
 * {@code ProductDetailsResponse}, <em>and</em> the mapping of that wire shape onto the domain's
 * {@link com.mycompany.myshop.backend.domain.entities.Product}. A supplier renaming a JSON field
 * fails here, which is the point of keeping the wire DTOs out of the centre.
 *
 * <p>Scoped to products only: the simulator's promotion endpoint is hardcoded and its error responses
 * cannot be provoked on demand, so a real-mode twin for those isn't buildable — see
 * {@code shop/plans/20260717-1015-component-stub-contract-beyond.md} item 4.
 */
abstract class BaseErpProductParityContractTest {

    protected abstract void arrangeProduct(String sku, String price);

    protected abstract ErpGateway erpGateway();

    @Test
    void getProductDetailsReturnsDetailsWhenFound() {
        arrangeProduct("BOOK-123", "10.00");

        var result = erpGateway().getProductDetails("BOOK-123");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("BOOK-123");
        assertThat(result.get().getPrice()).isEqualTo(Money.of("10.00"));
    }

    @Test
    void getProductDetailsReturnsEmptyWhenNotFound() {
        assertThat(erpGateway().getProductDetails("UNKNOWN-CONTRACT-SKU")).isEmpty();
    }
}
