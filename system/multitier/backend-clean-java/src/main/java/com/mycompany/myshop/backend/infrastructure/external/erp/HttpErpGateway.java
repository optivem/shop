package com.mycompany.myshop.backend.infrastructure.external.erp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myshop.backend.domain.entities.Product;
import com.mycompany.myshop.backend.domain.values.Money;
import com.mycompany.myshop.backend.domain.values.Promotion;
import com.mycompany.myshop.backend.domain.values.Rate;
import com.mycompany.myshop.backend.domain.values.Sku;
import com.mycompany.myshop.backend.domain.gateways.ErpGateway;
import com.mycompany.myshop.backend.infrastructure.external.ErpGatewayException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

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
    public Optional<Product> getProductDetails(Sku sku) {
        return fetchProductDetails(sku.value())
                .map(wire -> new Product(Sku.of(wire.getId()), Money.of(wire.getPrice())));
    }

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
                throw new ErpGatewayException("ERP API returned status " + response.statusCode()
                        + " for promotion. URL: " + url + ". Response: " + response.body());
            }

            return OBJECT_MAPPER.readValue(response.body(), GetPromotionResponse.class);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ErpGatewayException("Interrupted while fetching promotion details from URL: " + url, e);
        } catch (IOException e) {
            throw new ErpGatewayException("Failed to fetch promotion details from URL: " + url
                    + ". Error: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

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
                throw new ErpGatewayException("ERP API returned status " + response.statusCode() +
                        " for SKU: " + sku + ". URL: " + url + ". Response: " + response.body());
            }

            var result = OBJECT_MAPPER.readValue(response.body(), ProductDetailsResponse.class);
            return Optional.of(result);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ErpGatewayException("Interrupted while fetching product details for SKU: " + sku +
                    " from URL: " + url, e);
        } catch (IOException e) {
            throw new ErpGatewayException("Failed to fetch product details for SKU: " + sku +
                    " from URL: " + url +
                    ". Error: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }
}
