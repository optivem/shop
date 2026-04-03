package com.optivem.shop.monolith.controllers.web;

import com.optivem.shop.monolith.core.dtos.PlaceOrderRequest;
import com.optivem.shop.monolith.core.dtos.PlaceOrderResponse;
import com.optivem.shop.monolith.core.exceptions.ValidationException;
import com.optivem.shop.monolith.core.services.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ShopController {

    private static final String ATTR_ERROR = "error";
    private static final String ATTR_QUANTITY = "quantity";
    private static final String REDIRECT_NEW_ORDER = "redirect:/new-order";

    private final OrderService orderService;

    public ShopController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/new-order")
    public String shop() {
        return "new-order";
    }

    @PostMapping("/new-order")
    public String placeOrder(@RequestParam String sku,
                             @RequestParam String quantity,
                             RedirectAttributes redirectAttributes) {
        try {
            var request = new PlaceOrderRequest();
            request.setSku(sku);

            if (!trySetQuantity(request, quantity)) {
                return redirectWithError(redirectAttributes, "Quantity must be an integer", sku, quantity);
            }

            if (request.getQuantity() <= 0) {
                return redirectWithError(redirectAttributes, "Quantity must be positive", sku, quantity);
            }

            if (request.getSku() == null || request.getSku().isBlank()) {
                return redirectWithError(redirectAttributes, "SKU must not be empty", sku, quantity);
            }

            PlaceOrderResponse response = orderService.placeOrder(request);
            redirectAttributes.addFlashAttribute("success",
                    "Success! Order has been created with Order Number " + response.getOrderNumber());
            return REDIRECT_NEW_ORDER;
        } catch (ValidationException e) {
            return redirectWithError(redirectAttributes, e.getMessage(), sku, quantity);
        } catch (Exception e) {
            return redirectWithError(redirectAttributes, "An unexpected error occurred: " + e.getMessage(), sku, quantity);
        }
    }

    private boolean trySetQuantity(PlaceOrderRequest request, String quantity) {
        try {
            request.setQuantity(Integer.parseInt(quantity));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String redirectWithError(RedirectAttributes redirectAttributes, String errorMessage, String sku, String quantity) {
        redirectAttributes.addFlashAttribute(ATTR_ERROR, errorMessage);
        redirectAttributes.addFlashAttribute("sku", sku);
        redirectAttributes.addFlashAttribute(ATTR_QUANTITY, quantity);
        return REDIRECT_NEW_ORDER;
    }
}
