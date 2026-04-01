package com.optivem.eshop.systemtest.legacy.mod03.e2e;

import com.optivem.eshop.systemtest.legacy.mod03.e2e.base.BaseE2eTest;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.optivem.eshop.systemtest.commons.constants.Defaults.*;
import static org.assertj.core.api.Assertions.assertThat;

class PlaceOrderNegativeApiTest extends BaseE2eTest {
    @Override
    protected void setShopDriver() {
        setUpShopHttpClient();
    }

    @Test
    void shouldRejectOrderWithInvalidQuantity() throws Exception {
        var placeOrderJson = """
                {
                    "sku": "%s",
                    "quantity": "invalid-quantity",
                    "country": "%s"
                }
                """.formatted(createUniqueSku(SKU), COUNTRY);

        var response = shopApiHttpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(getShopApiBaseUrl() + "/api/orders"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(placeOrderJson))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertValidationError(response.statusCode(), response.body(), "quantity", "Quantity must be an integer");
    }

    @Test
    void shouldRejectOrderWithNonExistentSku() throws Exception {
        var placeOrderJson = """
                {
                    "sku": "NON-EXISTENT-SKU-12345",
                    "quantity": "%s",
                    "country": "%s"
                }
                """.formatted(QUANTITY, COUNTRY);

        var response = shopApiHttpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(getShopApiBaseUrl() + "/api/orders"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(placeOrderJson))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertValidationError(response.statusCode(), response.body(), "sku", "Product does not exist for SKU: NON-EXISTENT-SKU-12345");
    }

    @Test
    void shouldRejectOrderWithNegativeQuantity() throws Exception {
        var placeOrderJson = """
                {
                    "sku": "%s",
                    "quantity": "-10",
                    "country": "%s"
                }
                """.formatted(createUniqueSku(SKU), COUNTRY);

        var response = shopApiHttpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(getShopApiBaseUrl() + "/api/orders"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(placeOrderJson))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertValidationError(response.statusCode(), response.body(), "quantity", "Quantity must be positive");
    }

    @Test
    void shouldRejectOrderWithZeroQuantity() throws Exception {
        var placeOrderJson = """
                {
                    "sku": "%s",
                    "quantity": "0",
                    "country": "%s"
                }
                """.formatted(createUniqueSku(SKU), COUNTRY);

        var response = shopApiHttpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(getShopApiBaseUrl() + "/api/orders"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(placeOrderJson))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertValidationError(response.statusCode(), response.body(), "quantity", "Quantity must be positive");
    }

    @Test
    void shouldRejectOrderWithEmptySku() throws Exception {
        var placeOrderJson = """
                {
                    "sku": "",
                    "quantity": "%s",
                    "country": "%s"
                }
                """.formatted(QUANTITY, COUNTRY);

        var response = shopApiHttpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(getShopApiBaseUrl() + "/api/orders"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(placeOrderJson))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertValidationError(response.statusCode(), response.body(), "sku", "SKU must not be empty");
    }

    @Test
    void shouldRejectOrderWithEmptyQuantity() throws Exception {
        var placeOrderJson = """
                {
                    "sku": "%s",
                    "quantity": "",
                    "country": "%s"
                }
                """.formatted(createUniqueSku(SKU), COUNTRY);

        var response = shopApiHttpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(getShopApiBaseUrl() + "/api/orders"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(placeOrderJson))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertValidationError(response.statusCode(), response.body(), "quantity", "Quantity must not be empty");
    }

    @Test
    void shouldRejectOrderWithNonIntegerQuantity() throws Exception {
        var placeOrderJson = """
                {
                    "sku": "%s",
                    "quantity": "3.5",
                    "country": "%s"
                }
                """.formatted(createUniqueSku(SKU), COUNTRY);

        var response = shopApiHttpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(getShopApiBaseUrl() + "/api/orders"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(placeOrderJson))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertValidationError(response.statusCode(), response.body(), "quantity", "Quantity must be an integer");
    }

    @Test
    void shouldRejectOrderWithEmptyCountry() throws Exception {
        var placeOrderJson = """
                {
                    "sku": "%s",
                    "quantity": "%s",
                    "country": ""
                }
                """.formatted(createUniqueSku(SKU), QUANTITY);

        var response = shopApiHttpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(getShopApiBaseUrl() + "/api/orders"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(placeOrderJson))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertValidationError(response.statusCode(), response.body(), "country", "Country must not be empty");
    }

    @Test
    void shouldRejectOrderWithInvalidCountry() throws Exception {
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

        var createProductResponse = erpHttpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(getErpBaseUrl() + "/api/products"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(createProductJson))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(createProductResponse.statusCode()).isEqualTo(201);

        var placeOrderJson = """
                {
                    "sku": "%s",
                    "quantity": "%s",
                    "country": "XX"
                }
                """.formatted(sku, QUANTITY);

        var response = shopApiHttpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(getShopApiBaseUrl() + "/api/orders"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(placeOrderJson))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertValidationError(response.statusCode(), response.body(), "country", "Country does not exist: XX");
    }

    @Test
    void shouldRejectOrderWithNullQuantity() throws Exception {
        var placeOrderJson = """
                {
                    "sku": "%s",
                    "country": "%s",
                    "quantity": null
                }
                """.formatted(createUniqueSku(SKU), COUNTRY);

        var response = shopApiHttpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(getShopApiBaseUrl() + "/api/orders"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(placeOrderJson))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertValidationError(response.statusCode(), response.body(), "quantity", "Quantity must not be empty");
    }

    @Test
    void shouldRejectOrderWithNullSku() throws Exception {
        var placeOrderJson = """
                {
                    "sku": null,
                    "quantity": "%s",
                    "country": "%s"
                }
                """.formatted(QUANTITY, COUNTRY);

        var response = shopApiHttpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(getShopApiBaseUrl() + "/api/orders"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(placeOrderJson))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertValidationError(response.statusCode(), response.body(), "sku", "SKU must not be empty");
    }

    @Test
    void shouldRejectOrderWithNullCountry() throws Exception {
        var placeOrderJson = """
                {
                    "sku": "%s",
                    "quantity": "%s",
                    "country": null
                }
                """.formatted(createUniqueSku(SKU), QUANTITY);

        var response = shopApiHttpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(getShopApiBaseUrl() + "/api/orders"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(placeOrderJson))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertValidationError(response.statusCode(), response.body(), "country", "Country must not be empty");
    }

    private void assertValidationError(int statusCode, String responseBody, String field, String message) throws Exception {
        assertThat(statusCode).isEqualTo(422);
        var errorBody = httpObjectMapper.readTree(responseBody);
        assertThat(errorBody.get("detail").asText()).isEqualTo("The request contains one or more validation errors");
        var errors = errorBody.get("errors");
        assertThat(errors).isNotNull();
        assertThat(errors.isArray()).isTrue();
        boolean found = false;
        for (var error : errors) {
            if (error.get("field").asText().equals(field) && error.get("message").asText().equals(message)) {
                found = true;
                break;
            }
        }
        assertThat(found).isTrue();
    }
}

