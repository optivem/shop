package com.mycompany.myshop.testkit.driver.port.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Where the checkout left the customer: the checkout as it was opened and as it ended, the order confirmation, or the
 * cart; and, once the order was placed, the cart.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckOutResponse {
    public static final String CHECKOUT = "checkout";
    public static final String CONFIRMATION = "confirmation";
    public static final String CART = "cart";

    /** {@link #CHECKOUT}, {@link #CONFIRMATION} or {@link #CART}. */
    private String shownPage;
    /** The error notification's message on the checkout, or the cart's message; else null. */
    private String message;
    /** The error notification's lines under its message, as shown. */
    private List<String> messageDetails;

    // The checkout
    private List<ReviewLineDto> reviewLines;
    private String reviewSubtotal;
    /** The tax country and estimated tax rate when the checkout was opened, before any change. */
    private String openedTaxCountry;
    private String openedEstimatedTaxRate;
    private String taxCountry;
    private String estimatedTaxRate;
    private String reviewNote;
    private Boolean savedAddressSelected;
    /** The saved address as the checkout shows it; null when there is none. */
    private String savedAddress;

    // The confirmation
    /** The confirmation's heading; null when it thanks for no order. */
    private String thanks;
    /** The confirmed order numbers, in the order shown. */
    private List<ConfirmedOrderDto> confirmedOrders;
    private List<RejectedLineDto> rejectedLines;
    private Boolean viewMyOrdersOffered;
    private Boolean continueShoppingOffered;

    /** The cart after the order was placed (or rejected); null when no order was placed. */
    private List<ViewProductResponse.CartLineDto> cartLines;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewLineDto {
        private String sku;
        private Integer quantity;
        private String lineTotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfirmedOrderDto {
        private String orderNumber;
        /** Where the order number links to. */
        private String link;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RejectedLineDto {
        private String sku;
        private String reason;
    }
}
