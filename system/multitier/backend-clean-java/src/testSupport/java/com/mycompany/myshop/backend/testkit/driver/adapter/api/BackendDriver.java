package com.mycompany.myshop.backend.testkit.driver.adapter.api;

import com.mycompany.myshop.backend.usecases.queries.coupon.BrowseCouponsResponse;
import com.mycompany.myshop.backend.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.backend.usecases.queries.order.BrowseOrderHistoryResponse;
import com.mycompany.myshop.backend.usecases.commands.order.PlaceOrderRequest;
import com.mycompany.myshop.backend.usecases.commands.coupon.PublishCouponRequest;
import java.util.Map;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public class BackendDriver implements MyShopDriver {

    private final TestRestTemplate restTemplate;

    public BackendDriver(TestRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ResponseEntity<String> placeOrder(PlaceOrderRequest request) {
        return restTemplate.postForEntity("/api/orders", request, String.class);
    }

    public ResponseEntity<String> placeOrderRaw(String json) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity("/api/orders", new HttpEntity<>(json, headers), String.class);
    }

    public ResponseEntity<String> viewOrder(String orderNumber) {
        return restTemplate.getForEntity("/api/orders/" + orderNumber, String.class);
    }

    public ResponseEntity<String> cancelOrder(String orderNumber) {
        return restTemplate.postForEntity("/api/orders/" + orderNumber + "/cancel", null, String.class);
    }

    public ResponseEntity<BrowseOrderHistoryResponse> browseOrderHistory() {
        return restTemplate.getForEntity("/api/orders", BrowseOrderHistoryResponse.class);
    }

    public ResponseEntity<Void> publishCoupon(PublishCouponRequest request) {
        return restTemplate.postForEntity("/api/coupons", request, Void.class);
    }

    public ResponseEntity<BrowseCouponsResponse> browseCoupons() {
        return restTemplate.getForEntity("/api/coupons", BrowseCouponsResponse.class);
    }

    public ResponseEntity<Map<String, String>> checkHealth() {
        return restTemplate.exchange(
            "/health", HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
    }
}
