package com.optivem.shop.monolith.core.services.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.optivem.shop.monolith.core.dtos.external.ProductDetailsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

@Service
public class ErpGateway {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${erp.url}")
    private String erpUrl;

    public Optional<ProductDetailsResponse> getProductDetails(String sku) {
        var url = erpUrl + "/api/products/" + sku;

        try {
            var httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                return Optional.empty();
            }

            if (response.statusCode() != 200) {
                throw new IllegalStateException("ERP API returned status " + response.statusCode()
                        + " for SKU: " + sku + ". URL: " + url + ". Response: " + response.body());
            }

            var result = OBJECT_MAPPER.readValue(response.body(), ProductDetailsResponse.class);
            return Optional.of(result);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to fetch product details for SKU: " + sku
                    + " from URL: " + url
                    + ". Error: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch product details for SKU: " + sku
                    + " from URL: " + url
                    + ". Error: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }
}
