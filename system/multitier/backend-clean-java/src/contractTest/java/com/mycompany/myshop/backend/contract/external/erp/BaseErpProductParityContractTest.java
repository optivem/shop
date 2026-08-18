package com.mycompany.myshop.backend.contract.external.erp;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.domain.gateways.ErpGateway;
import com.mycompany.myshop.backend.domain.values.Money;
import org.junit.jupiter.api.Test;

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
