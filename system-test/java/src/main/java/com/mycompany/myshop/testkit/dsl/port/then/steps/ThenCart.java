package com.mycompany.myshop.testkit.dsl.port.then.steps;

import com.mycompany.myshop.testkit.dsl.port.then.steps.base.ThenStep;

/** The cart the visitor's action left. SKUs are aliases; titles, prices and messages are literals as shown. */
public interface ThenCart extends ThenStep<ThenCart> {
    ThenCart showsMessage(String expectedMessage);

    ThenCart hasCartBadge(int expectedCount);

    ThenCart hasNoCartBadge();

    /** The cart has exactly these lines, in this order; none for an empty cart. */
    ThenCart hasLines(String... skuAliases);

    ThenCart hasLine(String skuAlias, String expectedTitle, String expectedUnitPrice, int expectedQuantity, String expectedLineTotal);

    ThenCart hasLineQuantity(String skuAlias, int expectedQuantity);

    ThenCart hasLineTotal(String skuAlias, String expectedLineTotal);

    ThenCart hasLinePrices(String skuAlias, String expectedUnitPrice, String expectedLineTotal);

    /** The number of items (lines) the cart shows. */
    ThenCart hasItemCount(int expectedCount);

    ThenCart hasSubtotal(String expectedSubtotal);

    ThenCart saysTaxAndDiscountsAreCalculatedAtCheckout();

    ThenCart offersStartShopping();

    ThenCart didNotAskToSignIn();
}
