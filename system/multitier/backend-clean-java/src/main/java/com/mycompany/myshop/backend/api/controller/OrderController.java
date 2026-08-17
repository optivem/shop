package com.mycompany.myshop.backend.api.controller;

import com.mycompany.myshop.backend.core.dtos.BrowseOrderHistoryResponse;
import com.mycompany.myshop.backend.core.dtos.ViewOrderDetailsResponse;
import com.mycompany.myshop.backend.core.dtos.PlaceOrderRequest;
import com.mycompany.myshop.backend.core.dtos.PlaceOrderResponse;
import com.mycompany.myshop.backend.core.services.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/api/orders")
    public ResponseEntity<BrowseOrderHistoryResponse> browseOrderHistory(@RequestParam(required = false) String orderNumber) {
        var response = orderService.browseOrderHistory(orderNumber);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/orders")
    public ResponseEntity<PlaceOrderResponse> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        var response = orderService.placeOrder(request);
        var location = URI.create("/api/orders/" + response.getOrderNumber());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/api/orders/{orderNumber}")
    public ResponseEntity<ViewOrderDetailsResponse> getOrder(@PathVariable String orderNumber) {
        var response = orderService.getOrder(orderNumber);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/orders/{orderNumber}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable String orderNumber) {
        orderService.cancelOrder(orderNumber);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/orders/{orderNumber}/deliver")
    public ResponseEntity<Void> deliverOrder(@PathVariable String orderNumber) {
        orderService.deliverOrder(orderNumber);
        return ResponseEntity.noContent().build();
    }
}
