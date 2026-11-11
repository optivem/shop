package com.mycompany.myshop.testkit.driver.port.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * How the visitor manages the cart: a fresh browser session first gets the cart lines, each added from its product
 * page in the order given; then the action runs. With {@link #continueSession} the previous call's browser session
 * is kept instead, so only the action runs. Without an action, the call only fills the cart.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManageCartRequest {
    /** Opens the cart. */
    public static final String VIEW_CART = "view-cart";
    /** Opens the catalog. */
    public static final String BROWSE_CATALOG = "browse-catalog";
    /** Adds {@link #sku} with its card's "Add to cart" in the catalog. */
    public static final String ADD_FROM_CATALOG = "add-from-catalog";
    /** Adds {@link #quantity} of {@link #sku} from its product page. */
    public static final String ADD_FROM_PRODUCT_PAGE = "add-from-product-page";
    /** Types {@link #quantity} into the cart line of {@link #sku}. */
    public static final String CHANGE_QUANTITY = "change-quantity";
    /** Raises the cart line of {@link #sku} by 1 with the stepper. */
    public static final String INCREASE_QUANTITY = "increase-quantity";
    /** Lowers the cart line of {@link #sku} by 1 with the stepper. */
    public static final String DECREASE_QUANTITY = "decrease-quantity";
    /** Removes the cart line of {@link #sku}. */
    public static final String REMOVE_LINE = "remove-line";
    /** Leaves the site and comes back in the same browser. */
    public static final String REVISIT = "revisit";
    /** Signs in as the customer from the header. */
    public static final String SIGN_IN = "sign-in";

    private List<CartLineDto> cartLines;
    private Boolean continueSession;
    private String action;
    private String sku;
    private String quantity;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartLineDto {
        private String sku;
        private String quantity;
    }
}
