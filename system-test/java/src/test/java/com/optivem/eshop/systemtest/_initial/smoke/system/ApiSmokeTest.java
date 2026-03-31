package com.optivem.eshop.systemtest._initial.smoke.system;

import com.optivem.eshop.systemtest.configuration.ConfigurationLoader;
import com.optivem.eshop.systemtest.configuration.PropertyLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

class ApiSmokeTest {

    @Test
    void echo_shouldReturn200OK() throws Exception {
        var environment = PropertyLoader.getEnvironment(null);
        var externalSystemMode = PropertyLoader.getExternalSystemMode(null);
        var config = ConfigurationLoader.load(environment, externalSystemMode);

        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(config.getShopApiBaseUrl() + "/api/echo"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
    }
}
