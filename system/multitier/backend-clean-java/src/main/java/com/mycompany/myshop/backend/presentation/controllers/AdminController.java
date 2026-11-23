package com.mycompany.myshop.backend.presentation.controllers;

import com.mycompany.myshop.backend.presentation.UseCaseResponder;
import com.mycompany.myshop.backend.usecases.UseCase;
import com.mycompany.myshop.backend.usecases.commands.order.RecallSkuRequest;
import com.mycompany.myshop.backend.usecases.commands.order.SweepDeliveriesRequest;
import com.mycompany.myshop.backend.usecases.queries.order.BrowseCustomersRequest;
import com.mycompany.myshop.backend.usecases.queries.order.BrowseCustomersResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.mycompany.myshop.backend.usecases.commands.order.SweepDeliveriesResponse;
import com.mycompany.myshop.backend.usecases.commands.order.RecallSkuResponse;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UseCase<RecallSkuRequest, RecallSkuResponse> recallSku;
    private final UseCase<SweepDeliveriesRequest, SweepDeliveriesResponse> sweepDeliveries;
    private final UseCase<BrowseCustomersRequest, BrowseCustomersResponse> browseCustomers;
    private final UseCaseResponder responder;

    public AdminController(UseCase<RecallSkuRequest, RecallSkuResponse> recallSku, UseCase<SweepDeliveriesRequest, SweepDeliveriesResponse> sweepDeliveries,
                           UseCase<BrowseCustomersRequest, BrowseCustomersResponse> browseCustomers,
                           UseCaseResponder responder) {
        this.recallSku = recallSku;
        this.sweepDeliveries = sweepDeliveries;
        this.browseCustomers = browseCustomers;
        this.responder = responder;
    }

    @PostMapping("/recall/{sku}")
    public ResponseEntity<Object> recallSku(@PathVariable String sku) {
        var result = recallSku.execute(new RecallSkuRequest(sku));
        return responder.respond(result, ResponseEntity::ok);
    }

    @PostMapping("/orders/sweep-deliveries")
    public ResponseEntity<Object> sweepDeliveries(
            @RequestParam(required = false) Integer olderThanDays) {
        var result = sweepDeliveries.execute(new SweepDeliveriesRequest(olderThanDays));
        return responder.respond(result, ResponseEntity::ok);
    }

    // Admin-only in the other backends; open here, like every endpoint of this variant (see README).
    @GetMapping("/customers")
    public ResponseEntity<Object> browseCustomers() {
        var result = browseCustomers.execute(new BrowseCustomersRequest());
        return responder.respond(result, response -> ResponseEntity.ok(response.customers()));
    }
}
