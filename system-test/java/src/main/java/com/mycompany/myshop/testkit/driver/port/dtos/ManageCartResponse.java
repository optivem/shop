package com.mycompany.myshop.testkit.driver.port.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * The site after the visitor's action: the notification and cart badge where the action ended, then the cart as
 * the cart page shows it.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManageCartResponse {
    /** The action's notification; else the cart page's empty-cart or ERP-outage message; else null. */
    private String message;
    /** The header's cart badge where the action ended; 0 when it is not shown. */
    private Integer cartItemCount;
    /** The cart's lines, in the order shown. */
    private List<CartLineDto> lines;
    /** The "(n items)" count next to the heading; null when it is not shown. */
    private Integer itemCount;
    private String subtotal;
    /** The note that tax and discounts are calculated at checkout; null when it is not shown. */
    private String checkoutNote;
    private Boolean startShoppingOffered;
    /** Whether the visitor ended up on the login form. */
    private Boolean askedToSignIn;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartLineDto {
        private String sku;
        private String title;
        private String unitPrice;
        private Integer quantity;
        private String lineTotal;
    }
}
