package com.mycompany.myshop.backend.presentation.controller;

import com.mycompany.myshop.backend.usecases.dtos.BrowseOrderHistoryResponse;
import com.mycompany.myshop.backend.usecases.dtos.PlaceOrderRequest;
import com.mycompany.myshop.backend.usecases.dtos.PlaceOrderResponse;
import com.mycompany.myshop.backend.usecases.dtos.ViewOrderDetailsResponse;
import com.mycompany.myshop.backend.usecases.order.BrowseOrderHistory;
import com.mycompany.myshop.backend.usecases.order.CancelOrder;
import com.mycompany.myshop.backend.usecases.order.DeliverOrder;
import com.mycompany.myshop.backend.usecases.order.PlaceOrder;
import com.mycompany.myshop.backend.usecases.order.ViewOrderDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Binds HTTP to use cases and nothing else: no branching, no mapping, no arithmetic. Every method
 * is one call plus the status code the contract asks for.
 */
@RestController
public class OrderController {

    private final BrowseOrderHistory browseOrderHistory;
    private final PlaceOrder placeOrder;
    private final ViewOrderDetails viewOrderDetails;
    private final CancelOrder cancelOrder;
    private final DeliverOrder deliverOrder;

    public OrderController(BrowseOrderHistory browseOrderHistory, PlaceOrder placeOrder,
                           ViewOrderDetails viewOrderDetails, CancelOrder cancelOrder,
                           DeliverOrder deliverOrder) {
        this.browseOrderHistory = browseOrderHistory;
        this.placeOrder = placeOrder;
        this.viewOrderDetails = viewOrderDetails;
        this.cancelOrder = cancelOrder;
        this.deliverOrder = deliverOrder;
    }

    @GetMapping("/api/orders")
    public ResponseEntity<BrowseOrderHistoryResponse> browseOrderHistory(@RequestParam(required = false) String orderNumber) {
        var response = browseOrderHistory.execute(orderNumber);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/orders")
    public ResponseEntity<PlaceOrderResponse> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        var response = placeOrder.execute(request);
        var location = URI.create("/api/orders/" + response.getOrderNumber());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/api/orders/{orderNumber}")
    public ResponseEntity<ViewOrderDetailsResponse> getOrder(@PathVariable String orderNumber) {
        var response = viewOrderDetails.execute(orderNumber);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/orders/{orderNumber}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable String orderNumber) {
        cancelOrder.execute(orderNumber);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/orders/{orderNumber}/deliver")
    public ResponseEntity<Void> deliverOrder(@PathVariable String orderNumber) {
        deliverOrder.execute(orderNumber);
        return ResponseEntity.noContent().build();
    }
}
