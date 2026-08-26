package com.mycompany.myshop.backend.presentation.controllers;

import com.mycompany.myshop.backend.presentation.UseCaseResponder;
import com.mycompany.myshop.backend.usecases.UseCase;
import com.mycompany.myshop.backend.usecases.queries.order.BrowseOrderHistoryRequest;
import com.mycompany.myshop.backend.usecases.commands.order.CancelOrderRequest;
import com.mycompany.myshop.backend.usecases.commands.order.DeliverOrderRequest;
import com.mycompany.myshop.backend.usecases.commands.order.PlaceOrderRequest;
import com.mycompany.myshop.backend.usecases.queries.order.ViewOrderDetailsRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import com.mycompany.myshop.backend.usecases.queries.order.ViewOrderDetailsResponse;
import com.mycompany.myshop.backend.usecases.commands.order.PlaceOrderResponse;
import com.mycompany.myshop.backend.usecases.queries.order.BrowseOrderHistoryResponse;

@RestController
public class OrderController {

    private final UseCase<BrowseOrderHistoryRequest, BrowseOrderHistoryResponse> browseOrderHistory;
    private final UseCase<PlaceOrderRequest, PlaceOrderResponse> placeOrder;
    private final UseCase<ViewOrderDetailsRequest, ViewOrderDetailsResponse> viewOrderDetails;
    private final UseCase<CancelOrderRequest, Void> cancelOrder;
    private final UseCase<DeliverOrderRequest, Void> deliverOrder;
    private final UseCaseResponder responder;

    public OrderController(UseCase<BrowseOrderHistoryRequest, BrowseOrderHistoryResponse> browseOrderHistory, UseCase<PlaceOrderRequest, PlaceOrderResponse> placeOrder,
                           UseCase<ViewOrderDetailsRequest, ViewOrderDetailsResponse> viewOrderDetails, UseCase<CancelOrderRequest, Void> cancelOrder,
                           UseCase<DeliverOrderRequest, Void> deliverOrder,
                           UseCaseResponder responder) {
        this.browseOrderHistory = browseOrderHistory;
        this.placeOrder = placeOrder;
        this.viewOrderDetails = viewOrderDetails;
        this.cancelOrder = cancelOrder;
        this.deliverOrder = deliverOrder;
        this.responder = responder;
    }

    @GetMapping("/api/orders")
    public ResponseEntity<Object> browseOrderHistory(@RequestParam(required = false) String orderNumber,
                                                     @RequestParam(required = false) Integer page,
                                                     @RequestParam(required = false) Integer size) {
        var result = browseOrderHistory.execute(
                new BrowseOrderHistoryRequest(orderNumber, page, size));
        return responder.respond(result, response ->
                ResponseEntity.ok(response));
    }

    @PostMapping("/api/orders")
    public ResponseEntity<Object> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        var result = placeOrder.execute(request);
        return responder.respond(result, response -> ResponseEntity
                .created(URI.create("/api/orders/" + response.orderNumber()))
                .body(response));
    }

    @GetMapping("/api/orders/{orderNumber}")
    public ResponseEntity<Object> getOrder(@PathVariable String orderNumber) {
        var result = viewOrderDetails.execute(new ViewOrderDetailsRequest(orderNumber));
        return responder.respond(result, ResponseEntity::ok);
    }

    @PostMapping("/api/orders/{orderNumber}/cancel")
    public ResponseEntity<Object> cancelOrder(@PathVariable String orderNumber) {
        var result = cancelOrder.execute(new CancelOrderRequest(orderNumber));
        return responder.respond(result, ignored -> ResponseEntity.noContent().build());
    }

    @PostMapping("/api/orders/{orderNumber}/deliver")
    public ResponseEntity<Object> deliverOrder(@PathVariable String orderNumber) {
        var result = deliverOrder.execute(new DeliverOrderRequest(orderNumber));
        return responder.respond(result, ignored -> ResponseEntity.noContent().build());
    }
}
