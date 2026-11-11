package com.mycompany.myshop.testkit.dsl.port.when.steps;

import com.mycompany.myshop.testkit.dsl.port.when.steps.base.WhenStep;

/**
 * Views a product as a visitor (or as the logged-in user). The steps run in this order: put the cart line in the
 * cart, open the product (by its page address, or from the catalog), type a quantity, use the stepper, add to the
 * cart; then follow the link back to the catalog. SKUs are aliases.
 */
public interface WhenViewProduct extends WhenStep {
    WhenViewProduct withSku(String skuAlias);

    /** Opens the product from its card in the catalog instead of by its page address. */
    WhenViewProduct fromCatalog();

    /** Before viewing the product, the visitor's cart already contains this line. */
    WhenViewProduct withCartContaining(String skuAlias, int quantity);

    /** Types this quantity into the quantity field. */
    WhenViewProduct withQuantity(String quantity);

    WhenViewProduct withQuantityIncreased();

    WhenViewProduct withQuantityDecreased();

    /** Types this quantity and adds the product to the cart. */
    WhenViewProduct withAddedToCart(String quantity);

    /** After the other steps, follows the link back to the catalog. */
    WhenViewProduct withBackToCatalogFollowed();
}
