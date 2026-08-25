package com.mycompany.myshop.backend.infrastructure.external.erp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myshop.backend.domain.values.Product;
import com.mycompany.myshop.backend.domain.values.Money;
import com.mycompany.myshop.backend.domain.values.Promotion;
import com.mycompany.myshop.backend.domain.values.Rate;
import com.mycompany.myshop.backend.domain.values.Sku;
import com.mycompany.myshop.backend.domain.gateways.ErpGateway;
import com.mycompany.myshop.backend.domain.gateways.ErpGatewayException;
import com.mycompany.myshop.backend.infrastructure.external.GatewayHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
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

    // Private, and that is the whole point of the class. The two methods above are the boundary: they
    // are the only code that ever holds the ERP's JSON shape, and what they hand on is a domain value.
    // A wire DTO on a public signature -- even here in infrastructure, even "just for a test" -- makes
    // the supplier's field names reachable from somewhere else, and reachable is how they end up in
    // the centre. Anything that wants the raw response goes to the ERP itself, not through this class.
    private ErpGetPromotionResponse fetchPromotionDetails() {
        var url = erpUrl + "/api/promotion";

        try {
            var response = GatewayHttpClient.get(url);

            if (response.statusCode() != 200) {
                throw new ErpGatewayException("ERP API returned status " + response.statusCode()
                        + " for promotion. URL: " + url + ". Response: " + response.body());
            }

            return OBJECT_MAPPER.readValue(response.body(), ErpGetPromotionResponse.class);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ErpGatewayException("Interrupted while fetching promotion details from URL: " + url, e);
        } catch (IOException e) {
            throw new ErpGatewayException("Failed to fetch promotion details from URL: " + url
                    + ". Error: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    private Optional<ErpProductDetailsResponse> fetchProductDetails(String sku) {
        var url = erpUrl + "/api/products/" + sku;

        try {
            var response = GatewayHttpClient.get(url);

            if (response.statusCode() == 404) {
                return Optional.empty();
            }

            if (response.statusCode() != 200) {
                throw new ErpGatewayException("ERP API returned status " + response.statusCode() +
                        " for SKU: " + sku + ". URL: " + url + ". Response: " + response.body());
            }

            var result = OBJECT_MAPPER.readValue(response.body(), ErpProductDetailsResponse.class);
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
