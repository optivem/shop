package com.mycompany.myshop.backend.core.services.external;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myshop.backend.core.dtos.external.ErpGetPromotionResponse;
import com.mycompany.myshop.backend.core.dtos.external.ErpProductDetailsResponse;
import com.mycompany.myshop.backend.core.exceptions.ErpUnavailableException;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ErpGateway {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<ErpProductDetailsResponse>> PRODUCT_LIST_TYPE = new TypeReference<>() {
    };

    private final String erpUrl;
    private final ObservationRegistry observationRegistry;
    private final GatewayHttpClient http;

    public ErpGateway(String erpUrl) {
        this(erpUrl, ResilienceSettings.DEFAULTS);
    }

    public ErpGateway(String erpUrl, ResilienceSettings resilience) {
        this(erpUrl, ObservationRegistry.NOOP, resilience);
    }

    // The registry gives each outbound call a client span and a W3C traceparent header.
    @Autowired
    public ErpGateway(@Value("${erp.url}") String erpUrl, ObservationRegistry observationRegistry,
            ResilienceSettings resilience) {
        this.erpUrl = erpUrl;
        this.observationRegistry = observationRegistry;
        this.http = new GatewayHttpClient("erp", resilience);
    }

    public ErpGetPromotionResponse getPromotionDetails() {
        var url = erpUrl + "/api/promotion";

        try {
            var response = http.get(url, observationRegistry);

            if (response.statusCode() != 200) {
                throw new ErpUnavailableException("ERP API returned status " + response.statusCode()
                        + " for promotion. URL: " + url + ". Response: " + response.body());
            }

            return OBJECT_MAPPER.readValue(response.body(), ErpGetPromotionResponse.class);
        } catch (ErpUnavailableException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ErpUnavailableException("Failed to fetch promotion details from URL: " + url
                    + ". Error: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ErpUnavailableException("Failed to fetch promotion details from URL: " + url
                    + ". Error: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    public Optional<ErpProductDetailsResponse> getProductDetails(String sku) {
        var url = erpUrl + "/api/products/" + sku;

        try {
            var response = http.get(url, observationRegistry);

            if (response.statusCode() == 404) {
                return Optional.empty();
            }

            if (response.statusCode() != 200) {
                throw new ErpUnavailableException("ERP API returned status " + response.statusCode() +
                        " for SKU: " + sku + ". URL: " + url + ". Response: " + response.body());
            }

            var result = OBJECT_MAPPER.readValue(response.body(), ErpProductDetailsResponse.class);
            return Optional.of(result);
        } catch (ErpUnavailableException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ErpUnavailableException("Failed to fetch product details for SKU: " + sku +
                    " from URL: " + url +
                    ". Error: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ErpUnavailableException("Failed to fetch product details for SKU: " + sku +
                    " from URL: " + url +
                    ". Error: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    public List<ErpProductDetailsResponse> getProducts() {
        var url = erpUrl + "/api/products";

        try {
            var response = http.get(url, observationRegistry);

            if (response.statusCode() != 200) {
                throw new ErpUnavailableException("ERP API returned status " + response.statusCode() +
                        " for product list. URL: " + url + ". Response: " + response.body());
            }

            return OBJECT_MAPPER.readValue(response.body(), PRODUCT_LIST_TYPE);
        } catch (ErpUnavailableException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ErpUnavailableException("Failed to fetch product list from URL: " + url +
                    ". Error: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ErpUnavailableException("Failed to fetch product list from URL: " + url +
                    ". Error: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }
}
