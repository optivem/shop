package com.optivem.starter.backend.api.controller;

import com.optivem.starter.backend.core.dtos.BrowseOrderHistoryResponse;
import com.optivem.starter.backend.core.dtos.ViewOrderDetailsResponse;
import com.optivem.starter.backend.core.dtos.PlaceOrderRequest;
import com.optivem.starter.backend.core.dtos.PlaceOrderResponse;
import com.optivem.starter.backend.core.services.OrderService;
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
}
