package com.mycompany.myshop.backend.testkit.driver.adapter.external.tax.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class SimulatorTaxCountryClient {

    private final String baseUrl;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    public SimulatorTaxCountryClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void createCountry(String code, String taxRate) {
        var body = "{\"id\":\"" + code + "\",\"countryName\":\"" + code
            + "\",\"taxRate\":" + taxRate + "}";

        var created = send(HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/countries"))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body)));

        if (created.statusCode() / 100 == 2) {
            return;
        }

        var updated = send(HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/countries/" + code))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(body)));

        if (updated.statusCode() / 100 != 2) {
            throw new IllegalStateException("Failed to provision country " + code + " on the tax simulator. "
                + "POST status: " + created.statusCode() + " (" + created.body() + "); "
                + "PUT fallback status: " + updated.statusCode() + " (" + updated.body() + ")");
        }
    }

    private HttpResponse<String> send(HttpRequest.Builder request) {
        try {
            return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to reach the tax simulator.", e);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to reach the tax simulator. Is it running? Start it with: "
                + "docker compose -f docker/java/multitier/docker-compose.local.real.yml up external-system-simulators", e);
        }
    }
}
