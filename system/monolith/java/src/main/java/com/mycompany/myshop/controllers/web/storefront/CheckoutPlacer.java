package com.mycompany.myshop.controllers.web.storefront;

import com.mycompany.myshop.core.dtos.CurrentUser;
import com.mycompany.myshop.core.dtos.PlaceOrderRequest;
import com.mycompany.myshop.core.dtos.ShippingAddress;
import com.mycompany.myshop.core.exceptions.ErpUnavailableException;
import com.mycompany.myshop.core.exceptions.ValidationException;
import com.mycompany.myshop.core.services.OrderService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Places one order per cart line, in cart order, with the same shipping address and coupon (UC-009 BR-1,
 * storefront-ux §6.5). A rejected line doesn't stop the rest; its reason is the backend's own message, as the
 * place-order API would report it.
 */
public class CheckoutPlacer {

    // The same texts the place-order API answers with (GlobalExceptionHandler).
    static final String ERP_UNAVAILABLE = "Product information can't be retrieved right now";
    static final String UNEXPECTED_ERROR = "An unexpected error occurred. Please try again later.";

    private static final Logger log = LoggerFactory.getLogger(CheckoutPlacer.class);

    private final OrderService orderService;
    private final Validator validator;

    public CheckoutPlacer(OrderService orderService, Validator validator) {
        this.orderService = orderService;
        this.validator = validator;
    }

    public CheckoutResult place(List<Cart.CartLine> lines, ShippingAddress address, String couponCode, CurrentUser user) {
        var placed = new ArrayList<CheckoutResult.PlacedLine>();
        var rejected = new ArrayList<CheckoutResult.RejectedLine>();
        for (var line : lines) {
            var request = new PlaceOrderRequest();
            request.setSku(line.sku());
            request.setQuantity(line.quantity());
            request.setShippingAddress(address);
            request.setCouponCode(couponCode);
            var reason = placeOne(request, user, placed);
            if (reason != null) {
                rejected.add(new CheckoutResult.RejectedLine(line.sku(), line.quantity(), reason));
            }
        }
        return new CheckoutResult(List.copyOf(placed), List.copyOf(rejected));
    }

    /** Places the order and records it; returns null when placed, else why it was rejected. */
    private String placeOne(PlaceOrderRequest request, CurrentUser user, List<CheckoutResult.PlacedLine> placed) {
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            return violations.stream().map(ConstraintViolation::getMessage).sorted().collect(Collectors.joining("; "));
        }
        try {
            var response = orderService.placeOrder(request, user);
            placed.add(new CheckoutResult.PlacedLine(request.getSku(), request.getQuantity(), response.getOrderNumber()));
            return null;
        } catch (ValidationException e) {
            return e.getMessage();
        } catch (ErpUnavailableException e) {
            log.error("ERP unavailable while placing a checkout order", e);
            return ERP_UNAVAILABLE;
        } catch (RuntimeException e) {
            log.error("Unexpected error while placing a checkout order", e);
            return UNEXPECTED_ERROR;
        }
    }
}
