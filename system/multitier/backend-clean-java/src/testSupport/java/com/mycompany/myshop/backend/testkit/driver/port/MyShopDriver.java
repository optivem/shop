package com.mycompany.myshop.backend.testkit.driver.port;

import com.mycompany.myshop.backend.core.dtos.BrowseCouponsResponse;
import com.mycompany.myshop.backend.core.dtos.BrowseOrderHistoryResponse;
import com.mycompany.myshop.backend.core.dtos.PlaceOrderRequest;
import com.mycompany.myshop.backend.core.dtos.PublishCouponRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;

/**
 * What the use case DSL needs from the system under test. Mirrors the system-test project's
 * {@code testkit/driver/port/MyShopDriver}.
 *
 * <p>Weaker than its system-test counterpart in one respect: that port abstracts over a UI adapter and
 * an API adapter, and its return types are channel-neutral. This one has a single adapter and returns
 * Spring's {@link ResponseEntity}, so only a Spring-based adapter can implement it. It earns its place
 * as structural parity, not as an abstraction boundary — if a second channel ever lands here, the
 * return types are the first thing that has to change.
 */
public interface MyShopDriver {

    ResponseEntity<String> placeOrder(PlaceOrderRequest request);

    ResponseEntity<String> placeOrderRaw(String json);

    ResponseEntity<String> viewOrder(String orderNumber);

    /**
     * {@code String} rather than {@code Void}: cancellation is rejected by the year-end blackout and
     * by an already-cancelled order, and both surface as a {@code ProblemDetail} body. Binding to
     * {@code Void} would discard exactly what those scenarios assert on.
     */
    ResponseEntity<String> cancelOrder(String orderNumber);

    ResponseEntity<BrowseOrderHistoryResponse> browseOrderHistory();

    ResponseEntity<Void> publishCoupon(PublishCouponRequest request);

    ResponseEntity<BrowseCouponsResponse> browseCoupons();

    ResponseEntity<Map<String, String>> checkHealth();
}
