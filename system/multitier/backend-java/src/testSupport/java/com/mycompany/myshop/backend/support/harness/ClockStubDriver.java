package com.mycompany.myshop.backend.support.harness;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.client.WireMock;

/**
 * Low-level Clock stub driver. Registers the mapping against a supplied {@link WireMock} client; the
 * URL and JSON body are byte-identical to {@code AbstractComponentTest#stubClock}.
 */
public class ClockStubDriver {

    private final WireMock wireMock;

    public ClockStubDriver(WireMock wireMock) {
        this.wireMock = wireMock;
    }

    public void stubTime(String isoInstant) {
        wireMock.register(get(urlEqualTo("/api/time"))
            .willReturn(okJson("{\"time\":\"" + isoInstant + "\"}")));
    }

    public void stubTimeError(int status, String body) {
        wireMock.register(get(urlEqualTo("/api/time"))
            .willReturn(aResponse().withStatus(status).withBody(body)));
    }
}
