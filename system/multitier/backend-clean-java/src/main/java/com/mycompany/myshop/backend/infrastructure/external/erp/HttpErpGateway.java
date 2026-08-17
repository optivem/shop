package com.mycompany.myshop.backend.infrastructure.external.erp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myshop.backend.domain.entities.Product;
import com.mycompany.myshop.backend.domain.entities.Promotion;
import com.mycompany.myshop.backend.domain.values.Money;
import com.mycompany.myshop.backend.domain.values.Rate;
import com.mycompany.myshop.backend.domain.gateways.ErpGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * Talks HTTP to the ERP and maps its wire shapes to domain types. The "absent means 404" convention
 * is translated here — {@link #getProductDetails} returns an empty {@code Optional} for a 404 and
 * throws for any other non-200, so the domain never sees a status code.
 */
@Service
public class HttpErpGateway implements ErpGateway {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String erpUrl;

    public HttpErpGateway(@Value("${erp.url}") String erpUrl) {
        this.erpUrl = erpUrl;
    }

    @Override
    public Promotion getPromotionDetails() {
        var wire = fetchPromotionDetails();
        return new Promotion(wire.isPromotionActive(), Rate.of(wire.getDiscount()));
    }

    @Override
    public Optional<Product> getProductDetails(String sku) {
        return fetchProductDetails(sku)
                .map(wire -> new Product(wire.getId(), Money.of(wire.getPrice())));
    }

    /**
     * The raw ERP call and parse, before the mapping to {@link Promotion}. Exposed on the adapter
     * (rather than on the {@link ErpGateway} port) so the stub-contract component tests can read the
     * stub's bytes back through the SUT's real HTTP call and real Jackson parse.
     */
    public GetPromotionResponse fetchPromotionDetails() {
        var url = erpUrl + "/api/promotion";

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

            if (response.statusCode() != 200) {
                throw new IllegalStateException("ERP API returned status " + response.statusCode()
                        + " for promotion. URL: " + url + ". Response: " + response.body());
            }

            return OBJECT_MAPPER.readValue(response.body(), GetPromotionResponse.class);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to fetch promotion details from URL: " + url
                    + ". Error: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch promotion details from URL: " + url
                    + ". Error: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * The raw ERP call and parse, before the mapping to {@link Product}. See
     * {@link #fetchPromotionDetails} for why this is public on the adapter.
     */
    public Optional<ProductDetailsResponse> fetchProductDetails(String sku) {
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
                throw new IllegalStateException("ERP API returned status " + response.statusCode() +
                        " for SKU: " + sku + ". URL: " + url + ". Response: " + response.body());
            }

            var result = OBJECT_MAPPER.readValue(response.body(), ProductDetailsResponse.class);
            return Optional.of(result);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to fetch product details for SKU: " + sku +
                    " from URL: " + url +
                    ". Error: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch product details for SKU: " + sku +
                    " from URL: " + url +
                    ". Error: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }
}
