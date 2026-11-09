package com.mycompany.myshop.api.controller;

import com.mycompany.myshop.config.CurrentUserResolver;
import com.mycompany.myshop.core.dtos.BrowseOrderHistoryResponse;
import com.mycompany.myshop.core.dtos.PlaceOrderRequest;
import com.mycompany.myshop.core.dtos.PlaceOrderResponse;
import com.mycompany.myshop.core.dtos.ViewOrderDetailsResponse;
import com.mycompany.myshop.core.services.IdempotencyKeyStore;
import com.mycompany.myshop.core.services.IdempotentOrderService;
import com.mycompany.myshop.core.services.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class OrderApiController {

    private final OrderService orderService;
    private final IdempotentOrderService idempotentOrderService;

    public OrderApiController(OrderService orderService, IdempotentOrderService idempotentOrderService) {
        this.orderService = orderService;
        this.idempotentOrderService = idempotentOrderService;
    }

    @GetMapping("/api/orders")
    public ResponseEntity<BrowseOrderHistoryResponse> browseOrderHistory(@RequestParam(required = false) String orderNumber,
                                                                         @RequestParam(required = false) String customer,
                                                                         @RequestParam(required = false) Integer page,
                                                                         @RequestParam(required = false) Integer size,
                                                                         Authentication authentication) {
        var response = orderService.browseOrderHistory(orderNumber, customer, page, size, CurrentUserResolver.resolve(authentication));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/orders")
    public ResponseEntity<PlaceOrderResponse> placeOrder(@Valid @RequestBody PlaceOrderRequest request,
                                                         @RequestHeader(name = IdempotencyKeyStore.HEADER, required = false) String idempotencyKey,
                                                         Authentication authentication) {
        var result = idempotentOrderService.placeOrder(request, CurrentUserResolver.resolve(authentication), idempotencyKey);
        var location = URI.create("/api/orders/" + result.body().getOrderNumber());
        return ResponseEntity.status(result.status()).location(location).body(result.body());
    }

    @GetMapping("/api/orders/{orderNumber}")
    public ResponseEntity<ViewOrderDetailsResponse> getOrder(@PathVariable String orderNumber,
                                                             Authentication authentication) {
        var response = orderService.getOrder(orderNumber, CurrentUserResolver.resolve(authentication));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/orders/{orderNumber}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable String orderNumber,
                                            Authentication authentication) {
        orderService.cancelOrder(orderNumber, CurrentUserResolver.resolve(authentication));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/orders/{orderNumber}/deliver")
    public ResponseEntity<Void> deliverOrder(@PathVariable String orderNumber) {
        orderService.deliverOrder(orderNumber);
        return ResponseEntity.noContent().build();
    }
}
