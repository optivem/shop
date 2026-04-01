package com.optivem.eshop.systemtest.legacy.mod03.e2e;

import com.optivem.eshop.systemtest.legacy.mod03.e2e.base.BaseE2eTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.optivem.eshop.systemtest.commons.constants.Defaults.*;
import static org.assertj.core.api.Assertions.assertThat;

class PlaceOrderPositiveApiTest extends BaseE2eTest {
    @Override
    protected void setShopDriver() {
        setUpShopHttpClient();
    }

    @Test
    void shouldPlaceOrderWithCorrectSubtotalPrice() throws Exception {
        var sku = createUniqueSku(SKU);
        var createProductJson = """
                {
                    "id": "%s",
                    "title": "Test Product",
                    "description": "Test Description",
                    "category": "Test Category",
                    "brand": "Test Brand",
                    "price": "20.00"
                }
                """.formatted(sku);

        var createProductUri = URI.create(getErpBaseUrl() + "/api/products");
        var createProductRequest = HttpRequest.newBuilder()
                .uri(createProductUri)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(createProductJson))
                .build();

        var createProductResponse = erpHttpClient.send(createProductRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(createProductResponse.statusCode()).isEqualTo(201);

        var placeOrderJson = """
                {
                    "sku": "%s",
                    "quantity": "5",
                    "country": "%s"
                }
                """.formatted(sku, COUNTRY);

        var placeOrderUri = URI.create(getShopApiBaseUrl() + "/api/orders");
        var placeOrderRequest = HttpRequest.newBuilder()
                .uri(placeOrderUri)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(placeOrderJson))
                .build();

        var placeOrderResponse = shopApiHttpClient.send(placeOrderRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(placeOrderResponse.statusCode()).isEqualTo(201);

        var placeOrderBody = httpObjectMapper.readTree(placeOrderResponse.body());
        var orderNumber = placeOrderBody.get("orderNumber").asText();

        var viewOrderUri = URI.create(getShopApiBaseUrl() + "/api/orders/" + orderNumber);
        var viewOrderRequest = HttpRequest.newBuilder()
                .uri(viewOrderUri)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        var viewOrderResponse = shopApiHttpClient.send(viewOrderRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(viewOrderResponse.statusCode()).isEqualTo(200);

        var viewOrderBody = httpObjectMapper.readTree(viewOrderResponse.body());
        assertThat(viewOrderBody.get("subtotalPrice").asDouble()).isEqualTo(100.00);
    }

    @ParameterizedTest
    @CsvSource({
            "20.00, 5, 100.00",
            "10.00, 3, 30.00",
            "15.50, 4, 62.00",
            "99.99, 1, 99.99"
    })
    void shouldPlaceOrderWithCorrectSubtotalPriceParameterized(String unitPrice, String quantity, String expectedSubtotalPrice) throws Exception {
        var sku = createUniqueSku(SKU);
        var createProductJson = """
                {
                    "id": "%s",
                    "title": "Test Product",
                    "description": "Test Description",
                    "category": "Test Category",
                    "brand": "Test Brand",
                    "price": "%s"
                }
                """.formatted(sku, unitPrice);

        var createProductUri = URI.create(getErpBaseUrl() + "/api/products");
        var createProductRequest = HttpRequest.newBuilder()
                .uri(createProductUri)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(createProductJson))
                .build();

        var createProductResponse = erpHttpClient.send(createProductRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(createProductResponse.statusCode()).isEqualTo(201);

        var placeOrderJson = """
                {
                    "sku": "%s",
                    "quantity": "%s",
                    "country": "%s"
                }
                """.formatted(sku, quantity, COUNTRY);

        var placeOrderUri = URI.create(getShopApiBaseUrl() + "/api/orders");
        var placeOrderRequest = HttpRequest.newBuilder()
                .uri(placeOrderUri)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(placeOrderJson))
                .build();

        var placeOrderResponse = shopApiHttpClient.send(placeOrderRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(placeOrderResponse.statusCode()).isEqualTo(201);

        var placeOrderBody = httpObjectMapper.readTree(placeOrderResponse.body());
        var orderNumber = placeOrderBody.get("orderNumber").asText();

        var viewOrderUri = URI.create(getShopApiBaseUrl() + "/api/orders/" + orderNumber);
        var viewOrderRequest = HttpRequest.newBuilder()
                .uri(viewOrderUri)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        var viewOrderResponse = shopApiHttpClient.send(viewOrderRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(viewOrderResponse.statusCode()).isEqualTo(200);

        var viewOrderBody = httpObjectMapper.readTree(viewOrderResponse.body());
        var expectedSubtotal = Double.parseDouble(expectedSubtotalPrice);
        assertThat(viewOrderBody.get("subtotalPrice").asDouble()).isEqualTo(expectedSubtotal);
    }

    @Test
    void shouldPlaceOrder() throws Exception {
        var sku = createUniqueSku(SKU);
        var createProductJson = """
                {
                    "id": "%s",
                    "title": "Test Product",
                    "description": "Test Description",
                    "category": "Test Category",
                    "brand": "Test Brand",
                    "price": "20.00"
                }
                """.formatted(sku);

        var createProductUri = URI.create(getErpBaseUrl() + "/api/products");
        var createProductRequest = HttpRequest.newBuilder()
                .uri(createProductUri)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(createProductJson))
                .build();

        var createProductResponse = erpHttpClient.send(createProductRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(createProductResponse.statusCode()).isEqualTo(201);

        var placeOrderJson = """
                {
                    "sku": "%s",
                    "quantity": "5",
                    "country": "%s"
                }
                """.formatted(sku, COUNTRY);

        var placeOrderUri = URI.create(getShopApiBaseUrl() + "/api/orders");
        var placeOrderRequest = HttpRequest.newBuilder()
                .uri(placeOrderUri)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(placeOrderJson))
                .build();

        var placeOrderResponse = shopApiHttpClient.send(placeOrderRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(placeOrderResponse.statusCode()).isEqualTo(201);

        var placeOrderBody = httpObjectMapper.readTree(placeOrderResponse.body());
        var orderNumber = placeOrderBody.get("orderNumber").asText();
        assertThat(orderNumber).startsWith("ORD-");

        var viewOrderUri = URI.create(getShopApiBaseUrl() + "/api/orders/" + orderNumber);
        var viewOrderRequest = HttpRequest.newBuilder()
                .uri(viewOrderUri)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        var viewOrderResponse = shopApiHttpClient.send(viewOrderRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(viewOrderResponse.statusCode()).isEqualTo(200);

        var order = httpObjectMapper.readTree(viewOrderResponse.body());
        assertThat(order.get("orderNumber").asText()).isEqualTo(orderNumber);
        assertThat(order.get("sku").asText()).isEqualTo(sku);
        assertThat(order.get("country").asText()).isEqualTo(COUNTRY);
        assertThat(order.get("quantity").asInt()).isEqualTo(5);
        assertThat(order.get("unitPrice").asDouble()).isEqualTo(20.00);
        assertThat(order.get("subtotalPrice").asDouble()).isEqualTo(100.00);
        assertThat(order.get("status").asText()).isEqualTo("PLACED");
        assertThat(order.get("discountRate").asDouble()).isGreaterThanOrEqualTo(0.0);
        assertThat(order.get("discountAmount").asDouble()).isGreaterThanOrEqualTo(0.0);
        assertThat(order.get("taxRate").asDouble()).isGreaterThanOrEqualTo(0.0);
        assertThat(order.get("taxAmount").asDouble()).isGreaterThanOrEqualTo(0.0);
        assertThat(order.get("totalPrice").asDouble()).isGreaterThan(0.0);
    }
}

