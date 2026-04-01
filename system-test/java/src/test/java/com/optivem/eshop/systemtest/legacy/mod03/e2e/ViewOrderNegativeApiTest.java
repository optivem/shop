package com.optivem.eshop.systemtest.legacy.mod03.e2e;

import com.optivem.eshop.systemtest.legacy.mod03.e2e.base.BaseE2eTest;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

class ViewOrderNegativeApiTest extends BaseE2eTest {
    @Override
    protected void setShopDriver() {
        setUpShopHttpClient();
    }

    @Test
    void shouldNotBeAbleToViewNonExistentOrder() throws Exception {
        var orderNumber = "NON-EXISTENT-ORDER-99999";

        var viewOrderUri = URI.create(getShopApiBaseUrl() + "/api/orders/" + orderNumber);
        var viewOrderRequest = HttpRequest.newBuilder()
                .uri(viewOrderUri)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        var viewOrderResponse = shopApiHttpClient.send(viewOrderRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(viewOrderResponse.statusCode()).isEqualTo(404);

        var errorBody = httpObjectMapper.readTree(viewOrderResponse.body());
        assertThat(errorBody.get("detail").asText()).isEqualTo("Order NON-EXISTENT-ORDER-99999 does not exist.");
    }
}

