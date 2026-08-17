package com.mycompany.myshop.backend.testkit.driver.adapter.external.erp.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Provisions a product fixture directly against the real ERP simulator ({@code external-systems/simulators/mock-server.js}),
 * the same way system-test's {@code ErpRealClient} does (same {@code client} sub-package convention).
 * Sits alongside, not inside, {@code driver/adapter/external/erp}: it does not implement
 * {@link com.mycompany.myshop.backend.testkit.driver.port.external.erp.ErpDriver} — that port's vocabulary
 * is stub-programming only (see its Javadoc) and is shared with {@code componentTest}, which is permanently
 * stub-only by design — this class only needs to write one thing (a product) to a real backing, not honor
 * the full stub-programming contract.
 */
public class ErpRealClient {

    private final String baseUrl;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    public ErpRealClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Creates a product fixture, or updates it in place if a previous run already created it. The
     * simulator's {@code products} resource does not upsert on {@code POST} — a second {@code POST} for
     * the same {@code id} fails with {@code 500 Insert failed, duplicate id} — so a rejected create falls
     * back to {@code PUT} against that {@code id}, which the simulator treats as a replace.
     */
    public void createProduct(String sku, String price) {
        var body = "{\"id\":\"" + sku + "\",\"title\":\"Test Product Title\","
            + "\"description\":\"Test Product Description\",\"category\":\"Test Category\","
            + "\"brand\":\"Test Brand\",\"price\":" + price + "}";

        var created = send(HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/products"))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body)));

        if (created.statusCode() / 100 == 2) {
            return;
        }

        var updated = send(HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/products/" + sku))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(body)));

        if (updated.statusCode() / 100 != 2) {
            throw new IllegalStateException("Failed to provision product " + sku + " on the ERP simulator. "
                + "POST status: " + created.statusCode() + " (" + created.body() + "); "
                + "PUT fallback status: " + updated.statusCode() + " (" + updated.body() + ")");
        }
    }

    private HttpResponse<String> send(HttpRequest.Builder request) {
        try {
            return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to reach the ERP simulator.", e);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to reach the ERP simulator. Is it running? Start it with: "
                + "docker compose -f docker/java/multitier/docker-compose.local.real.yml up external-system-simulators", e);
        }
    }
}
