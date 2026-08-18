package com.mycompany.myshop.backend.presentation.controller;

import com.mycompany.myshop.backend.presentation.UseCaseResponder;
import com.mycompany.myshop.backend.usecases.dtos.BrowseOrderHistoryRequest;
import com.mycompany.myshop.backend.usecases.dtos.CancelOrderRequest;
import com.mycompany.myshop.backend.usecases.dtos.DeliverOrderRequest;
import com.mycompany.myshop.backend.usecases.dtos.PlaceOrderRequest;
import com.mycompany.myshop.backend.usecases.dtos.ViewOrderDetailsRequest;
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
 * is one call plus the status code the contract asks for — what a refusal looks like is
 * {@link UseCaseResponder}'s job, uniformly, for every use case here.
 */
@RestController
public class OrderController {

    private final BrowseOrderHistory browseOrderHistory;
    private final PlaceOrder placeOrder;
    private final ViewOrderDetails viewOrderDetails;
    private final CancelOrder cancelOrder;
    private final DeliverOrder deliverOrder;
    private final UseCaseResponder responder;

    public OrderController(BrowseOrderHistory browseOrderHistory, PlaceOrder placeOrder,
                           ViewOrderDetails viewOrderDetails, CancelOrder cancelOrder,
                           DeliverOrder deliverOrder, UseCaseResponder responder) {
        this.browseOrderHistory = browseOrderHistory;
        this.placeOrder = placeOrder;
        this.viewOrderDetails = viewOrderDetails;
        this.cancelOrder = cancelOrder;
        this.deliverOrder = deliverOrder;
        this.responder = responder;
    }

    @GetMapping("/api/orders")
    public ResponseEntity<Object> browseOrderHistory(@RequestParam(required = false) String orderNumber) {
        var result = browseOrderHistory.execute(new BrowseOrderHistoryRequest(orderNumber));
        return responder.respond(result, ResponseEntity::ok);
    }

    @PostMapping("/api/orders")
    public ResponseEntity<Object> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        var result = placeOrder.execute(request);
        return responder.respond(result, response -> ResponseEntity
                .created(URI.create("/api/orders/" + response.getOrderNumber()))
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
