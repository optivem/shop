package com.mycompany.myshop.backend.testkit.driver.adapter.api;

import com.mycompany.myshop.backend.core.dtos.BrowseCouponsResponse;
import com.mycompany.myshop.backend.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.backend.core.dtos.BrowseOrderHistoryResponse;
import com.mycompany.myshop.backend.core.dtos.PlaceOrderRequest;
import com.mycompany.myshop.backend.core.dtos.PublishCouponRequest;
import java.util.Map;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Low-level driver for the system under test (the backend itself), wrapping the {@link
 * TestRestTemplate} bound to the random component-test port. Owns the raw HTTP mechanics of the
 * order endpoints — {@code POST /api/orders} and {@code GET /api/orders/{orderNumber}} — and the
 * coupon endpoints — {@code POST /api/coupons} and {@code GET /api/coupons} — mirroring how {@link
 * ErpStubDriver} / {@link TaxStubDriver} / {@link ClockStubDriver} own the WireMock mechanics of the
 * external systems. Also owns {@code GET /health} — the dedicated liveness probe used by the harness
 * smoke test.
 *
 * <p>The two endpoints that can answer with either a success payload or an RFC 7807 error — place
 * order and view order — are fetched as raw {@code String} rather than deserialized straight into
 * their success DTO. Binding them to the success type would discard the {@code ProblemDetail} body on
 * a 4xx (Jackson would quietly map it onto a null-filled success DTO), leaving the DSL nothing to
 * assert a rejection message against. The driver hands the body up untouched; the use case layer owns
 * the parse, picking the success DTO or the {@code ProblemDetail} once the test has said which
 * outcome it expects.
 */
public class BackendDriver implements MyShopDriver {

    private final TestRestTemplate restTemplate;

    public BackendDriver(TestRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ResponseEntity<String> placeOrder(PlaceOrderRequest request) {
        return restTemplate.postForEntity("/api/orders", request, String.class);
    }

    /**
     * Posts a body the typed {@link PlaceOrderRequest} cannot express. {@code quantity} is an {@code
     * Integer} on the DTO, so a non-integer ({@code "3.5"}, {@code "lala"}) or a blank quantity can
     * only reach the controller as raw JSON — which is exactly what the API channel sends, and the
     * only way to exercise Jackson's type-level rejection ({@code @TypeValidationMessage}).
     */
    public ResponseEntity<String> placeOrderRaw(String json) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity("/api/orders", new HttpEntity<>(json, headers), String.class);
    }

    public ResponseEntity<String> viewOrder(String orderNumber) {
        return restTemplate.getForEntity("/api/orders/" + orderNumber, String.class);
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
