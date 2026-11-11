package com.mycompany.myshop.testkit.dsl.port.then.steps;

import com.mycompany.myshop.testkit.dsl.port.then.steps.base.ThenStep;

/** The product page the executed view showed. SKUs are aliases; titles, prices and messages are literals as shown. */
public interface ThenProductPage extends ThenStep<ThenProductPage> {
    ThenProductPage hasTitle(String expectedTitle);

    ThenProductPage hasBrand(String expectedBrand);

    ThenProductPage hasCategory(String expectedCategory);

    ThenProductPage hasSku(String expectedSkuAlias);

    ThenProductPage hasPrice(String expectedPrice);

    ThenProductPage hasDescription(String expectedDescription);

    /** The breadcrumb trail, in order. */
    ThenProductPage hasBreadcrumbs(String... expectedBreadcrumbs);

    ThenProductPage hasQuantity(int expectedQuantity);

    ThenProductPage showsMessage(String expectedMessage);

    ThenProductPage offersBackToCatalog();

    ThenProductPage doesNotOfferAddToCart();

    ThenProductPage hasCartBadge(int expectedCount);

    ThenProductPage isOnPageOfProduct(String skuAlias);

    /** The visitor's cart has exactly this line. */
    ThenProductPage cartContains(String skuAlias, int expectedQuantity);

    ThenProductPage cartIsEmpty();

    /** After following the link back, the catalog shows exactly these products, in this order. */
    ThenProductPage catalogShowsProducts(String... skuAliases);

    /** The rest of the site keeps working: the acting user can still open their order history. */
    ThenProductPage canOpenMyOrders();
}
