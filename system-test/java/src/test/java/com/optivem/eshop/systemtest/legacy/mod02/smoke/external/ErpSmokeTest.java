package com.optivem.eshop.systemtest.legacy.mod02.smoke.external;

import com.optivem.eshop.systemtest.legacy.mod02.base.BaseRawTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

class ErpSmokeTest extends BaseRawTest {
    private static final String HEALTH_ENDPOINT = "/health";

    @BeforeEach
    void setUp() {
        setUpExternalHttpClients();
    }

    @Test
    void shouldBeAbleToGoToErp() throws Exception {
        var uri = URI.create(getErpBaseUrl() + HEALTH_ENDPOINT);
        var request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        var response = erpHttpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
    }
}


