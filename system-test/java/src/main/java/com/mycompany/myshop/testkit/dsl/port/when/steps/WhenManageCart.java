package com.mycompany.myshop.testkit.dsl.port.when.steps;

import com.mycompany.myshop.testkit.dsl.port.when.steps.base.WhenStep;

/**
 * The visitor manages the cart. The steps run in this order: put the cart lines in the cart (each from its product
 * page, in the order given); change the ERP; then the visitor's one action, which is viewing the cart unless
 * another is given. The cart is read last. SKUs are aliases.
 */
public interface WhenManageCart extends WhenStep {
    /** Before the visitor acts, the cart already contains this line. */
    WhenManageCart withCartContaining(String skuAlias, int quantity);

    /** After the cart is filled, the ERP's price of this product changes. */
    WhenManageCart withErpPriceChanged(String skuAlias, String unitPrice);

    /** After the cart is filled, the ERP becomes unavailable. */
    WhenManageCart withErpUnavailable();

    /** After the cart is filled, the visitor views it while the ERP is unavailable; then the ERP is available again. */
    WhenManageCart withCartViewedWhileErpUnavailable();

    WhenManageCart withCatalogBrowsed();

    /** Adds the product with its card's "Add to cart" in the catalog (quantity 1). */
    WhenManageCart withAddedFromCatalog(String skuAlias);

    WhenManageCart withAddedFromProductPage(String skuAlias, String quantity);

    /** Types this quantity into the product's cart line. */
    WhenManageCart withQuantityChanged(String skuAlias, String quantity);

    WhenManageCart withQuantityIncreased(String skuAlias);

    WhenManageCart withQuantityDecreased(String skuAlias);

    WhenManageCart withLineRemoved(String skuAlias);

    /** Leaves the site and comes back later in the same browser. */
    WhenManageCart withSiteLeftAndRevisited();

    WhenManageCart withSignedInAsCustomer();
}
