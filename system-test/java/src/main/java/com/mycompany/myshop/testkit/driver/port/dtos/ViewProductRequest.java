package com.mycompany.myshop.testkit.driver.port.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * How the visitor views a product, applied in this order: put the cart line in the cart, open the product (by its
 * page address, or from the catalog), type a quantity, use the stepper, add to the cart; then the follow-up, if any.
 * Unset inputs are left untouched. Only the SKU reaches the API: the rest are UI controls.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewProductRequest {
    /** Raises the quantity by 1 with the stepper. */
    public static final String INCREASE = "increase";
    /** Lowers the quantity by 1 with the stepper. */
    public static final String DECREASE = "decrease";
    /** Follows the link back to the catalog. */
    public static final String BACK_TO_CATALOG = "back-to-catalog";

    private String sku;
    /** Opens the product from its card in the catalog instead of by its page address. */
    private Boolean fromCatalog;
    /** A product already in the cart before the visitor views this one, with {@link #cartQuantity}. */
    private String cartSku;
    private String cartQuantity;
    /** The quantity typed into the quantity field. */
    private String quantity;
    /** {@link #INCREASE} or {@link #DECREASE}. */
    private String quantityStep;
    /** Adds the product to the cart with the quantity in the field. */
    private Boolean addToCart;
    /** {@link #BACK_TO_CATALOG}, run after the other inputs. */
    private String followUp;
}
