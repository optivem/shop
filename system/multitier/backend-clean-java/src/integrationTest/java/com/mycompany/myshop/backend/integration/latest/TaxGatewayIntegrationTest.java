package com.mycompany.myshop.backend.integration.latest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycompany.myshop.backend.domain.values.Country;
import com.mycompany.myshop.backend.domain.gateways.TaxGatewayException;
import com.mycompany.myshop.backend.domain.gateways.TaxGateway;
import com.mycompany.myshop.backend.domain.values.Rate;
import com.mycompany.myshop.backend.integration.latest.base.BaseGatewayIntegrationTest;
import org.junit.jupiter.api.Test;

class TaxGatewayIntegrationTest extends BaseGatewayIntegrationTest {

    private final TaxGateway taxGateway = taxGateway();

    @Test
    void getTaxDetailsReturnsRateWhenCountryKnown() {
        tax().returnsTaxRate().country("US").taxRate("0.10").execute();

        var result = taxGateway.getTaxDetails(Country.of("US"));

        assertThat(result).isPresent();
        assertThat(result.get().countryName()).isEqualTo(Country.of("US"));
        assertThat(result.get().rate()).isEqualTo(Rate.of("0.10"));
    }

    @Test
    void getTaxDetailsReturnsEmptyWhenCountryUnknown() {
        tax().returnsNoTaxRate().country("ZZ").execute();

        assertThat(taxGateway.getTaxDetails(Country.of("ZZ"))).isEmpty();
    }

    @Test
    void getTaxDetailsThrowsOnServerError() {
        tax().failsForCountry().country("US").status(500).body("Internal Server Error").execute();

        assertThatThrownBy(() -> taxGateway.getTaxDetails(Country.of("US")))
            .isInstanceOf(TaxGatewayException.class)
            .hasMessageContaining("500");
    }

    @Test
    void getTaxDetailsThrowsOnServiceUnavailable() {
        tax().failsForCountry().country("US").status(503).body("Service Unavailable").execute();

        assertThatThrownBy(() -> taxGateway.getTaxDetails(Country.of("US")))
            .isInstanceOf(TaxGatewayException.class)
            .hasMessageContaining("503");
    }
}
