package com.optivem.starter.systemtest.smoketests;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

class ApiSmokeTest {

    @Test
    void echo_shouldReturn200OK() throws Exception {
        // DISCLAIMER: This is an example of a badly written test
        // which unfortunately simulates real-life software test projects.
        // This is the starting point for our Starter exercises.

        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:8080/api/echo"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
    }
}
