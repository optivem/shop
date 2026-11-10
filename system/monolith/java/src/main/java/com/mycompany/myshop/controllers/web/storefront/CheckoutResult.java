package com.mycompany.myshop.controllers.web.storefront;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * The outcome of a checkout: the order number of each placed line and the backend's reason for each rejected one.
 * Kept in the session for the confirmation page, so a reload still shows it.
 */
public record CheckoutResult(List<PlacedLine> placed, List<RejectedLine> rejected) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String SESSION_ATTRIBUTE = "myshop.lastCheckout";

    public List<String> placedSkus() {
        return placed.stream().map(PlacedLine::sku).toList();
    }

    public record PlacedLine(String sku, int quantity, String orderNumber) implements Serializable {
    }

    public record RejectedLine(String sku, int quantity, String reason) implements Serializable {
    }
}
