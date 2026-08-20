package com.mycompany.myshop.backend.integration.latest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycompany.myshop.backend.domain.gateways.ErpGateway;
import com.mycompany.myshop.backend.domain.values.Money;
import com.mycompany.myshop.backend.domain.values.Rate;
import com.mycompany.myshop.backend.domain.values.Sku;
import com.mycompany.myshop.backend.domain.gateways.ErpGatewayException;
import com.mycompany.myshop.backend.integration.latest.base.BaseGatewayIntegrationTest;
import org.junit.jupiter.api.Test;

class ErpGatewayIntegrationTest extends BaseGatewayIntegrationTest {

    private final ErpGateway erpGateway = erpGateway();

    @Test
    void getProductDetailsReturnsDetailsWhenFound() {
        erp().returnsProduct().sku("BOOK-123").unitPrice("10.00").execute();

        var result = erpGateway.getProductDetails(Sku.of("BOOK-123"));

        assertThat(result).isPresent();
        assertThat(result.get().getSku()).isEqualTo(Sku.of("BOOK-123"));
        assertThat(result.get().getPrice()).isEqualTo(Money.of("10.00"));
    }

    @Test
    void getProductDetailsReturnsEmptyWhenNotFound() {
        erp().returnsNoProduct().sku("UNKNOWN").execute();

        assertThat(erpGateway.getProductDetails(Sku.of("UNKNOWN"))).isEmpty();
    }

    @Test
    void getProductDetailsThrowsOnServerError() {
        erp().failsForProduct().sku("BAD-SKU").status(500).body("Internal Server Error").execute();

        assertThatThrownBy(() -> erpGateway.getProductDetails(Sku.of("BAD-SKU")))
            .isInstanceOf(ErpGatewayException.class)
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
            .isInstanceOf(ErpGatewayException.class)
            .hasMessageContaining("503");
    }
}
