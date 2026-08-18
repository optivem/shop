package com.mycompany.myshop.backend.testkit.driver.port;

import com.mycompany.myshop.backend.usecases.coupon.BrowseCouponsResponse;
import com.mycompany.myshop.backend.usecases.order.BrowseOrderHistoryResponse;
import com.mycompany.myshop.backend.usecases.order.PlaceOrderRequest;
import com.mycompany.myshop.backend.usecases.coupon.PublishCouponRequest;
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
