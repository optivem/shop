package com.mycompany.myshop.api.controller;

import com.mycompany.myshop.core.services.OrderService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminApiController {

    private final OrderService orderService;

    public AdminApiController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** The usernames that have placed an order, distinct, A to Z (UC-011 customer filter). */
    @GetMapping("/customers")
    public List<String> browseCustomers() {
        return orderService.browseCustomers();
    }
}
