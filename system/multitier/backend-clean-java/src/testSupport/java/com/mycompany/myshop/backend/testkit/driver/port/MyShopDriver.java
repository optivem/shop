package com.mycompany.myshop.backend.testkit.driver.port;

import com.mycompany.myshop.backend.usecases.queries.coupon.BrowseCouponsResponse;
import com.mycompany.myshop.backend.usecases.queries.order.BrowseOrderHistoryResponse;
import com.mycompany.myshop.backend.usecases.commands.order.PlaceOrderRequest;
import com.mycompany.myshop.backend.usecases.commands.coupon.PublishCouponRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;

public interface MyShopDriver {

    ResponseEntity<String> placeOrder(PlaceOrderRequest request);

    ResponseEntity<String> placeOrderRaw(String json);

    ResponseEntity<String> viewOrder(String orderNumber);

    ResponseEntity<String> cancelOrder(String orderNumber);

    ResponseEntity<BrowseOrderHistoryResponse> browseOrderHistory();

    ResponseEntity<Void> publishCoupon(PublishCouponRequest request);

    ResponseEntity<BrowseCouponsResponse> browseCoupons();

    ResponseEntity<Map<String, String>> checkHealth();
}
