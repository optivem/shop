package com.mycompany.myshop.testkit.driver.port.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * How the customer checks out: a fresh browser session first gets the cart lines, each added from its product page
 * in the order given (with {@link #continueSession} the previous call's session is kept instead). Then the checkout
 * is opened, by its address or from the cart; with {@link #shippingAddress} the customer ships to that address
 * instead of the saved one; with {@link #placeOrder} the order is placed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckOutRequest {
    /** Opens the checkout by its page address. */
    public static final String OPEN = "open";
    /** Opens the cart and follows its "Proceed to checkout". */
    public static final String PROCEED_FROM_CART = "proceed-from-cart";

    private List<CartLineDto> cartLines;
    private Boolean continueSession;
    private String entry;
    /** After proceeding to the login form, signs in as the customer there. */
    private Boolean signIn;
    /** Ship to this address instead of the saved one; the country is a code as the form offers it. */
    private ShippingAddress shippingAddress;
    private String couponCode;
    private Boolean placeOrder;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartLineDto {
        private String sku;
        private String quantity;
    }
}
