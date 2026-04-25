package com.mycompany.myshop.controllers.web;

import com.mycompany.myshop.core.exceptions.NotExistValidationException;
import com.mycompany.myshop.core.services.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class OrderDetailsController {

    private final OrderService orderService;

    public OrderDetailsController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/order-details/{orderNumber}")
    public String orderDetails(@PathVariable String orderNumber, Model model) {
        try {
            var order = orderService.getOrder(orderNumber);
            model.addAttribute("order", order);
        } catch (NotExistValidationException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "order-details";
    }
}
