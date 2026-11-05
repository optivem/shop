package com.mycompany.myshop.controllers.web;

import com.mycompany.myshop.config.CurrentUserResolver;
import com.mycompany.myshop.core.exceptions.NotExistValidationException;
import com.mycompany.myshop.core.services.OrderService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class OrderDetailsController {

    private final OrderService orderService;

    public OrderDetailsController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/order-details")
    public String orderDetails(@RequestParam(required = false) String orderNumber, Model model, Authentication authentication) {
        if (orderNumber == null || orderNumber.isBlank()) {
            return "order-details";
        }
        try {
            var order = orderService.getOrder(orderNumber, CurrentUserResolver.resolve(authentication));
            model.addAttribute("order", order);
        } catch (NotExistValidationException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "order-details";
    }
}
