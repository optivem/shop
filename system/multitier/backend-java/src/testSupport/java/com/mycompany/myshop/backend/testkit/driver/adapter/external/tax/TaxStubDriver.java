package com.mycompany.myshop.backend.testkit.driver.adapter.external.tax;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.mycompany.myshop.backend.testkit.driver.port.external.tax.TaxDriver;

/**
 * Low-level Tax stub driver. Registers mappings against a supplied {@link WireMock} client; that the
 * URL and JSON body match what the SUT actually consumes is guarded by
 * {@code TaxStubContractComponentTest}.
 */
public class TaxStubDriver implements TaxDriver {

    private final WireMock wireMock;

    public TaxStubDriver(WireMock wireMock) {
        this.wireMock = wireMock;
    }

    /** See {@code ErpStubDriver#goToErp}. */
    public void goToTax() {
        wireMock.allStubMappings();
    }

    public void returnsTaxRate(String country, String rate) {
        wireMock.register(get(urlEqualTo("/api/countries/" + country))
            .willReturn(okJson("{\"id\":\"" + country + "\",\"countryName\":\"" + country
                + "\",\"taxRate\":" + rate + "}")));
    }

    /** A 404 from Tax is what makes {@code taxGateway.getTaxDetails} empty — the trigger for
     * "Country does not exist". */
    public void returnsNoTaxRate(String country) {
        wireMock.register(get(urlEqualTo("/api/countries/" + country))
            .willReturn(aResponse().withStatus(404)));
    }

    public void failsForCountry(String country, int status, String body) {
        wireMock.register(get(urlEqualTo("/api/countries/" + country))
            .willReturn(aResponse().withStatus(status).withBody(body)));
    }
}
