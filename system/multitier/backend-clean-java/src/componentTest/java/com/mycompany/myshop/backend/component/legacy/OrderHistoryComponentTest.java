package com.mycompany.myshop.backend.component.legacy;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.BaseComponentTest;
import com.mycompany.myshop.backend.usecases.queries.order.BrowseOrderHistoryItemResponse;
import com.mycompany.myshop.backend.usecases.queries.order.BrowseOrderHistoryResponse;
import com.mycompany.myshop.backend.usecases.commands.order.PlaceOrderRequest;
import com.mycompany.myshop.backend.usecases.commands.order.PlaceOrderResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class OrderHistoryComponentTest extends BaseComponentTest {

    @Test
    void browseReturnsPlacedOrders() {
        var orderNumber = placeOrder();

        var response = restTemplate.getForEntity("/api/orders", BrowseOrderHistoryResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().orders())
            .extracting(BrowseOrderHistoryItemResponse::orderNumber)
            .contains(orderNumber);
    }

    @Test
    void viewMissingOrderReturnsNotFound() {
        var response = restTemplate.getForEntity("/api/orders/UNKNOWN", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("Order UNKNOWN does not exist.");
    }

    private String placeOrder() {
        CLOCK.stubFor(get(urlEqualTo("/api/time"))
            .willReturn(okJson("{\"time\":\"2026-03-10T12:00:00Z\"}")));
        ERP.stubFor(get(urlEqualTo("/api/products/BOOK-123"))
            .willReturn(okJson("{\"id\":\"BOOK-123\",\"price\":10.00}")));
        ERP.stubFor(get(urlEqualTo("/api/promotion"))
            .willReturn(okJson("{\"promotionActive\":false,\"discount\":1.0}")));
        TAX.stubFor(get(urlEqualTo("/api/countries/US"))
            .willReturn(okJson("{\"id\":\"US\",\"countryName\":\"US\",\"taxRate\":0.10}")));

        var request = new PlaceOrderRequest();
        request.setSku("BOOK-123");
        request.setQuantity(2);
        request.setCountry("US");

        var placed = restTemplate.postForEntity("/api/orders", request, PlaceOrderResponse.class);
        assertThat(placed.getBody()).isNotNull();
        return placed.getBody().orderNumber();
    }
}
