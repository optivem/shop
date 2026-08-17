package com.mycompany.myshop.backend.integration.latest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycompany.myshop.backend.domain.gateways.ErpGateway;
import com.mycompany.myshop.backend.domain.values.Money;
import com.mycompany.myshop.backend.domain.values.Rate;
import com.mycompany.myshop.backend.integration.latest.base.BaseGatewayIntegrationTest;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.erp.ErpDsl;
import org.junit.jupiter.api.Test;

/**
 * "After" of the external-systems contract-tests refactor at the narrow-integration layer: identical
 * scenarios to the {@code legacy/} twin, but the ERP happy/404 stubs are declared through the shared
 * use case DSL under {@code support/} — the same {@link ErpDsl} the component {@code latest/} tests
 * reach as {@code app.erp()}. A narrow-integration test drives one gateway, not a scenario, so it
 * uses the use case layer directly and never sees the scenario DSL above it. Every stub — including
 * the 500/503 error-injection cases — is programmed through the DSL; no raw WireMock survives here,
 * which is the whole point of the "after".
 *
 * <p>The harness (in-process WireMock, stub-side DSL, SUT-side gateway) comes from
 * {@link BaseGatewayIntegrationTest}, so what remains below is the scenarios themselves.
 *
 * <p>The assertions read the domain types the {@link ErpGateway} port promises — {@link Money} and
 * {@link Rate}, not the {@code BigDecimal}s of the ERP's wire shape. So this pins the adapter's
 * mapping as well as its parse: a wire field the adapter stopped reading shows up here as a wrong
 * {@code Money}, not as a silently absent JSON property.
 */
class ErpGatewayIntegrationTest extends BaseGatewayIntegrationTest {

    private final ErpGateway erpGateway = erpGateway();

    @Test
    void getProductDetailsReturnsDetailsWhenFound() {
        erp().returnsProduct().sku("BOOK-123").unitPrice("10.00").execute();

        var result = erpGateway.getProductDetails("BOOK-123");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("BOOK-123");
        assertThat(result.get().getPrice()).isEqualTo(Money.of("10.00"));
    }

    @Test
    void getProductDetailsReturnsEmptyWhenNotFound() {
        erp().returnsNoProduct().sku("UNKNOWN").execute();

        assertThat(erpGateway.getProductDetails("UNKNOWN")).isEmpty();
    }

    @Test
    void getProductDetailsThrowsOnServerError() {
        erp().failsForProduct().sku("BAD-SKU").status(500).body("Internal Server Error").execute();

        assertThatThrownBy(() -> erpGateway.getProductDetails("BAD-SKU"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("500");
    }

    @Test
    void getPromotionDetailsReturnsPromotion() {
        erp().returnsPromotion().active(true).discount("0.15").execute();

        var result = erpGateway.getPromotionDetails();

        assertThat(result.isActive()).isTrue();
        assertThat(result.getDiscount()).isEqualTo(Rate.of("0.15"));
    }

    @Test
    void getPromotionDetailsThrowsOnServerError() {
        erp().failsForPromotion().status(503).body("Service Unavailable").execute();

        assertThatThrownBy(() -> erpGateway.getPromotionDetails())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("503");
    }
}
