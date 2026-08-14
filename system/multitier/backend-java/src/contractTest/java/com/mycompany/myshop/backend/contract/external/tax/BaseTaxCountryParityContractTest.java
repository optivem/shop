package com.mycompany.myshop.backend.contract.external.tax;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.core.services.external.TaxGateway;
import org.junit.jupiter.api.Test;

/**
 * Shared shape for the tax country contract: does the production {@link TaxGateway}'s parse still
 * agree with whatever is answering at {@link #taxGateway()}'s URL? {@link TaxStubParityContractTest}
 * answers that against our own stub; {@link TaxRealParityContractTest} answers it against the real
 * tax simulator. Both provision-and-pin the same exact values, so the assertions below don't need to
 * know which mode is running.
 *
 * <p>The ERP twin ({@code BaseErpProductParityContractTest}) is the model for this; see its Javadoc
 * for why the pair exists at all.
 *
 * <p>Arranges a country code the simulator does not seed, rather than reusing one of its built-in
 * rows: the point is to pin what {@code TaxStubDriver} writes against what the real system accepts
 * and returns, which is only meaningful if both sides provision the same value.
 */
abstract class BaseTaxCountryParityContractTest {

    protected static final String ARRANGED_COUNTRY = "ZZ";
    protected static final String ARRANGED_RATE = "0.11";
    protected static final String UNKNOWN_COUNTRY = "UNKNOWN-CONTRACT-COUNTRY";

    protected abstract void arrangeCountry(String code, String taxRate);

    protected abstract TaxGateway taxGateway();

    @Test
    void getTaxDetailsReturnsDetailsWhenFound() {
        arrangeCountry(ARRANGED_COUNTRY, ARRANGED_RATE);

        var result = taxGateway().getTaxDetails(ARRANGED_COUNTRY);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(ARRANGED_COUNTRY);
        assertThat(result.get().getTaxRate()).isEqualByComparingTo(ARRANGED_RATE);
    }

    @Test
    void getTaxDetailsReturnsEmptyWhenNotFound() {
        assertThat(taxGateway().getTaxDetails(UNKNOWN_COUNTRY)).isEmpty();
    }
}
