package com.optivem.shop.systemtest.legacy.mod03.e2e;

import com.optivem.shop.systemtest.legacy.mod03.e2e.base.BaseE2eTest;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static com.optivem.shop.systemtest.commons.constants.Defaults.*;
import static org.assertj.core.api.Assertions.assertThat;

class PlaceOrderPositiveApiTest extends BaseE2eTest {
    @Override
    protected void setShopClient() {
        setUpShopHttpClient();
    }

    @Test
    void shouldPlaceOrderForValidInput() throws Exception {
        var sku = SKU + "-" + UUID.randomUUID().toString().substring(0, 8);
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
        assertThat(order.get("quantity").asInt()).isEqualTo(5);
        assertThat(order.get("unitPrice").asDouble()).isEqualTo(20.00);
        assertThat(order.get("basePrice").asDouble()).isEqualTo(100.00);
        assertThat(order.get("totalPrice").asDouble()).isGreaterThan(0);
        assertThat(order.get("status").asText()).isEqualTo("PLACED");
    }
}
