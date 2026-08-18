package com.mycompany.myshop.backend.contract.external.tax;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.domain.gateways.TaxGateway;
import com.mycompany.myshop.backend.domain.values.Country;
import com.mycompany.myshop.backend.domain.values.Rate;
import org.junit.jupiter.api.Test;

abstract class BaseTaxCountryParityContractTest {

    protected static final String ARRANGED_COUNTRY = "ZZ";
    protected static final String ARRANGED_RATE = "0.11";
    protected static final String UNKNOWN_COUNTRY = "UNKNOWN-CONTRACT-COUNTRY";

    protected abstract void arrangeCountry(String code, String taxRate);

    protected abstract TaxGateway taxGateway();

    @Test
    void getTaxDetailsReturnsDetailsWhenFound() {
        arrangeCountry(ARRANGED_COUNTRY, ARRANGED_RATE);

        var result = taxGateway().getTaxDetails(Country.of(ARRANGED_COUNTRY));

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(ARRANGED_COUNTRY);
        assertThat(result.get().getRate()).isEqualTo(Rate.of(ARRANGED_RATE));
    }

    @Test
    void getTaxDetailsReturnsEmptyWhenNotFound() {
        assertThat(taxGateway().getTaxDetails(Country.of(UNKNOWN_COUNTRY))).isEmpty();
    }
}
