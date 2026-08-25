package com.mycompany.myshop.backend.api.controller;

import com.mycompany.myshop.backend.core.dtos.RecallSkuResponse;
import com.mycompany.myshop.backend.core.services.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final OrderService orderService;

    public AdminController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/recall/{sku}")
    public ResponseEntity<RecallSkuResponse> recallSku(@PathVariable String sku) {
        var response = orderService.recallSku(sku);
        return ResponseEntity.ok(response);
    }
}
