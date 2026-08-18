package com.mycompany.myshop.backend.testkit.driver.adapter.external.clock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.mycompany.myshop.backend.testkit.driver.port.external.clock.ClockDriver;

public class ClockStubDriver implements ClockDriver {

    private final WireMock wireMock;

    public ClockStubDriver(WireMock wireMock) {
        this.wireMock = wireMock;
    }

    public void goToClock() {
        wireMock.allStubMappings();
    }

    public void returnsTime(String isoInstant) {
        wireMock.register(get(urlEqualTo("/api/time"))
            .willReturn(okJson("{\"time\":\"" + isoInstant + "\"}")));
    }

    public void failsForTime(int status, String body) {
        wireMock.register(get(urlEqualTo("/api/time"))
            .willReturn(aResponse().withStatus(status).withBody(body)));
    }
}
