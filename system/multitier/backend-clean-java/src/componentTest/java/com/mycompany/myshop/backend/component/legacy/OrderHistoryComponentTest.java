package com.mycompany.myshop.backend.component.legacy;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.BaseComponentTest;
import com.mycompany.myshop.backend.usecases.order.BrowseOrderHistoryResponse;
import com.mycompany.myshop.backend.usecases.order.PlaceOrderRequest;
import com.mycompany.myshop.backend.usecases.order.PlaceOrderResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * "Before" of the external-systems contract-tests refactor: browse order history and view-details
 * flows with the ERP / Tax / Clock externals stubbed by raw, inlined WireMock. The {@code latest/}
 * twin drives the identical scenarios through the shared stub DSL.
 */
class OrderHistoryComponentTest extends BaseComponentTest {

    @Test
    void browseReturnsPlacedOrders() {
        var orderNumber = placeOrder();

        var response = restTemplate.getForEntity("/api/orders", BrowseOrderHistoryResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getOrders())
            .extracting(BrowseOrderHistoryResponse.BrowseOrderHistoryItemResponse::getOrderNumber)
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
        return placed.getBody().getOrderNumber();
    }
}
