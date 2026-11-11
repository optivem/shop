package com.mycompany.myshop.testkit.dsl.port.then.steps;

import com.mycompany.myshop.testkit.dsl.port.then.steps.base.ThenStep;

/**
 * Where the checkout left the customer: the checkout, the order confirmation or the cart. SKUs are aliases; messages
 * may name SKU and coupon aliases; prices, tax rates and addresses are literals as shown.
 */
public interface ThenCheckout extends ThenStep<ThenCheckout> {
    /** The customer is still on (or was shown) the checkout. */
    ThenCheckout isShown();

    ThenCheckout showsTheCart();

    /** The checkout's order review has exactly these lines, in this order. */
    ThenCheckout hasReviewLines(String... skuAliases);

    ThenCheckout hasReviewLine(String skuAlias, int expectedQuantity, String expectedLineTotal);

    ThenCheckout hasReviewQuantity(String skuAlias, int expectedQuantity);

    ThenCheckout hasReviewSubtotal(String expectedSubtotal);

    /** The order review's tax country and estimated tax rate (e.g. "19%") when the checkout was opened. */
    ThenCheckout hadTaxWhenOpened(String expectedCountry, String expectedRate);

    /** The order review's tax country and estimated tax rate (e.g. "19%") as the checkout ended. */
    ThenCheckout hasTax(String expectedCountry, String expectedRate);

    ThenCheckout saysDiscountAndTaxAreWorkedOutPerOrder();

    /** The saved address, as the checkout shows it, is selected. */
    ThenCheckout hasSavedAddressSelected(String expectedAddress);

    ThenCheckout showsMessage(String expectedMessage);

    /** One of the lines under the checkout's error message, such as "City is required". */
    ThenCheckout showsMessageDetail(String expectedDetail);

    /** The checkout tells the customer this line was rejected ("SKU: reason" under its message). */
    ThenCheckout saysRejected(String skuAlias, String expectedReason);

    ThenCheckout thanksForTheOrder();

    /** The confirmation lists exactly one order for each of these products, in this order. */
    ThenCheckout confirmsOrdersFor(String... skuAliases);

    ThenCheckout confirmsNoOrder();

    ThenCheckout linksEachOrderToItsDetails();

    ThenCheckout offersViewMyOrdersAndContinueShopping();

    /** The confirmation shows this line was rejected, for this reason. */
    ThenCheckout confirmsRejected(String skuAlias, String expectedReason);

    ThenCheckout confirmsNothingRejected();

    /** After the order was placed, the cart has exactly these lines, in this order; none for an empty cart. */
    ThenCheckout hasCartLines(String... skuAliases);

    ThenCheckout hasCartLineQuantity(String skuAlias, int expectedQuantity);
}
