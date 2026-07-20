package com.mycompany.myshop.backend.support.harness;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.client.WireMock;

/**
 * Low-level Tax stub driver. Registers mappings against a supplied {@link WireMock} client; the URL
 * and JSON body are byte-identical to {@code AbstractComponentTest#stubTax}.
 */
public class TaxStubDriver {

    private final WireMock wireMock;

    public TaxStubDriver(WireMock wireMock) {
        this.wireMock = wireMock;
    }

    public void stubTax(String country, String rate) {
        wireMock.register(get(urlEqualTo("/api/countries/" + country))
            .willReturn(okJson("{\"id\":\"" + country + "\",\"countryName\":\"" + country
                + "\",\"taxRate\":" + rate + "}")));
    }

    /** A 404 from Tax is what makes {@code taxGateway.getTaxDetails} empty — the trigger for
     * "Country does not exist". */
    public void stubTaxMissing(String country) {
        wireMock.register(get(urlEqualTo("/api/countries/" + country))
            .willReturn(aResponse().withStatus(404)));
    }

    public void stubTaxError(String country, int status, String body) {
        wireMock.register(get(urlEqualTo("/api/countries/" + country))
            .willReturn(aResponse().withStatus(status).withBody(body)));
    }
}
