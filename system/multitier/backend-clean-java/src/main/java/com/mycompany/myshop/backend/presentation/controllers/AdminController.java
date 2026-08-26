package com.mycompany.myshop.backend.presentation.controllers;

import com.mycompany.myshop.backend.presentation.UseCaseResponder;
import com.mycompany.myshop.backend.usecases.UseCase;
import com.mycompany.myshop.backend.usecases.commands.order.RecallSkuRequest;
import com.mycompany.myshop.backend.usecases.commands.order.SweepDeliveriesRequest;
import org.springframework.http.ResponseEntity;
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
    private final UseCaseResponder responder;

    public AdminController(UseCase<RecallSkuRequest, RecallSkuResponse> recallSku, UseCase<SweepDeliveriesRequest, SweepDeliveriesResponse> sweepDeliveries,
                           UseCaseResponder responder) {
        this.recallSku = recallSku;
        this.sweepDeliveries = sweepDeliveries;
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
}
