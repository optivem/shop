package com.mycompany.myshop.controllers.web;

import com.mycompany.myshop.config.CurrentUserResolver;
import com.mycompany.myshop.core.services.OrderService;
import org.springframework.security.core.Authentication;
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
    public String orderHistory(@RequestParam(required = false) String orderNumber, Model model,
                               Authentication authentication) {
        var response = orderService.browseOrderHistory(orderNumber, CurrentUserResolver.resolve(authentication));
        model.addAttribute("orders", response.getOrders());
        model.addAttribute("admin", CurrentUserResolver.isAdmin(authentication));
        model.addAttribute("filter", orderNumber != null ? orderNumber : "");
        return "order-history";
    }
}
