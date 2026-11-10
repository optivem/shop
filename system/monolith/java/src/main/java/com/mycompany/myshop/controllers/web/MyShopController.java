package com.mycompany.myshop.controllers.web;

import com.mycompany.myshop.config.CurrentUserResolver;
import com.mycompany.myshop.core.dtos.PlaceOrderRequest;
import com.mycompany.myshop.core.dtos.PlaceOrderResponse;
import com.mycompany.myshop.core.exceptions.ValidationException;
import com.mycompany.myshop.core.services.OrderService;
import com.mycompany.myshop.core.services.ProfileService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class MyShopController {

    private static final String ATTR_ERROR = "error";
    private static final String ATTR_QUANTITY = "quantity";
    private static final String REDIRECT_NEW_ORDER = "redirect:/new-order";
    private static final String DEFAULT_COUNTRY = "US";

    private final OrderService orderService;
    private final ProfileService profileService;

    public MyShopController(OrderService orderService, ProfileService profileService) {
        this.orderService = orderService;
        this.profileService = profileService;
    }

    /** Quick order, with its Country prefilled from the customer's saved profile (UC-001 1a, UC-010 BR-5). */
    @GetMapping("/new-order")
    public String myShop(Model model, Authentication authentication) {
        model.addAttribute("defaultCountry", defaultCountry(authentication));
        return "new-order";
    }

    private String defaultCountry(Authentication authentication) {
        try {
            var profile = profileService.getProfile(CurrentUserResolver.resolve(authentication));
            var saved = profile == null || profile.getCountry() == null ? "" : profile.getCountry().trim();
            return saved.isEmpty() ? DEFAULT_COUNTRY : saved;
        } catch (RuntimeException e) {
            return DEFAULT_COUNTRY;
        }
    }

    @PostMapping("/new-order")
    public String placeOrder(@RequestParam String sku,
                             @RequestParam String quantity,
                             @RequestParam(required = false, defaultValue = "") String country,
                             @RequestParam(required = false, defaultValue = "") String couponCode,
                             RedirectAttributes redirectAttributes,
                             Authentication authentication) {
        try {
            var request = new PlaceOrderRequest();
            request.setSku(sku);

            if (!trySetQuantity(request, quantity)) {
                return redirectWithError(redirectAttributes, "Quantity must be an integer", sku, quantity, country, couponCode);
            }

            if (request.getQuantity() <= 0) {
                return redirectWithError(redirectAttributes, "Quantity must be positive", sku, quantity, country, couponCode);
            }

            if (request.getSku() == null || request.getSku().isBlank()) {
                return redirectWithError(redirectAttributes, "SKU must not be empty", sku, quantity, country, couponCode);
            }

            if (country == null || country.isBlank()) {
                return redirectWithError(redirectAttributes, "Country must not be empty", sku, quantity, country, couponCode);
            }

            request.setCountry(country);
            request.setCouponCode(couponCode.isBlank() ? null : couponCode);

            PlaceOrderResponse response = orderService.placeOrder(request, CurrentUserResolver.resolve(authentication));
            redirectAttributes.addFlashAttribute("success",
                    "Success! Order has been created with Order Number " + response.getOrderNumber());
            return REDIRECT_NEW_ORDER;
        } catch (ValidationException e) {
            return redirectWithError(redirectAttributes, e.getMessage(), sku, quantity, country, couponCode);
        } catch (Exception e) {
            return redirectWithError(redirectAttributes, "An unexpected error occurred: " + e.getMessage(), sku, quantity, country, couponCode);
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

    private String redirectWithError(RedirectAttributes redirectAttributes, String errorMessage,
                                     String sku, String quantity, String country, String couponCode) {
        redirectAttributes.addFlashAttribute(ATTR_ERROR, errorMessage);
        redirectAttributes.addFlashAttribute("sku", sku);
        redirectAttributes.addFlashAttribute(ATTR_QUANTITY, quantity);
        redirectAttributes.addFlashAttribute("country", country);
        redirectAttributes.addFlashAttribute("couponCode", couponCode);
        return REDIRECT_NEW_ORDER;
    }
}
