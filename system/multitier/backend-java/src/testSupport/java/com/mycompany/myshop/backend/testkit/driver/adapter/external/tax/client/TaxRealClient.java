package com.mycompany.myshop.backend.testkit.driver.adapter.external.tax.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Provisions a country fixture directly against the real tax simulator
 * ({@code external-systems/simulators/mock-server.js}), the tax counterpart of
 * {@code ErpRealClient} and following the same {@code client} sub-package convention.
 *
 * <p>Sits alongside, not inside, {@code driver/adapter/external/tax}: it does not implement
 * {@link com.mycompany.myshop.backend.testkit.driver.port.external.tax.TaxDriver} — that port's
 * vocabulary is stub-programming only and is shared with {@code componentTest}, which is permanently
 * stub-only by design. This class only needs to write one thing (a country) to a real backing.
 */
public class TaxRealClient {

    private final String baseUrl;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    public TaxRealClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Creates a country fixture, or updates it in place if it already exists. The simulator seeds a
     * handful of countries and its {@code countries} resource does not upsert on {@code POST} — a
     * {@code POST} for an existing {@code id} fails — so a rejected create falls back to {@code PUT}
     * against that {@code id}, which the simulator treats as a replace. Mirrors
     * {@code ErpRealClient#createProduct}.
     */
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
