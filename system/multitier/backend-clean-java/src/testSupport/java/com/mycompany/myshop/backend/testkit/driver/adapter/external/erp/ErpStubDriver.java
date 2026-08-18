package com.mycompany.myshop.backend.testkit.driver.adapter.external.erp;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.mycompany.myshop.backend.testkit.driver.port.external.erp.ErpDriver;

public class ErpStubDriver implements ErpDriver {

    private final WireMock wireMock;

    public ErpStubDriver(WireMock wireMock) {
        this.wireMock = wireMock;
    }

    public void goToErp() {
        wireMock.allStubMappings();
    }

    public void returnsProduct(String sku, String price) {
        wireMock.register(get(urlEqualTo("/api/products/" + sku))
            .willReturn(okJson("{\"id\":\"" + sku + "\",\"price\":" + price + "}")));
    }

    public void returnsNoProduct(String sku) {
        wireMock.register(get(urlEqualTo("/api/products/" + sku))
            .willReturn(aResponse().withStatus(404)));
    }

    public void returnsPromotion(boolean active, String discount) {
        wireMock.register(get(urlEqualTo("/api/promotion"))
            .willReturn(okJson("{\"promotionActive\":" + active + ",\"discount\":" + discount + "}")));
    }

    public void failsForProduct(String sku, int status, String body) {
        wireMock.register(get(urlEqualTo("/api/products/" + sku))
            .willReturn(aResponse().withStatus(status).withBody(body)));
    }

    public void failsForPromotion(int status, String body) {
        wireMock.register(get(urlEqualTo("/api/promotion"))
            .willReturn(aResponse().withStatus(status).withBody(body)));
    }
}
