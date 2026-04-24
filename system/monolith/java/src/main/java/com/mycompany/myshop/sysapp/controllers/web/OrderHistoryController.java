package com.mycompany.myshop.sysapp.controllers.web;

import com.mycompany.myshop.sysapp.core.services.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class OrderHistoryController {

    private final OrderService orderService;

    public OrderHistoryController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/order-history")
    public String orderHistory(@RequestParam(required = false) String orderNumber, Model model) {
        var response = orderService.browseOrderHistory(orderNumber);
        model.addAttribute("orders", response.getOrders());
        model.addAttribute("filter", orderNumber != null ? orderNumber : "");
        return "order-history";
    }
}
