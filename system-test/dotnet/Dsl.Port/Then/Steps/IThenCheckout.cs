using System.Runtime.CompilerServices;

namespace Dsl.Port.Then.Steps;

/// <summary>
/// Where the checkout left the customer: the checkout, the order confirmation or the cart. SKUs are aliases; messages
/// may name SKU and coupon aliases; prices, tax rates and addresses are literals as shown.
/// </summary>
public interface IThenCheckout
{
    /// <summary>The customer is still on (or was shown) the checkout.</summary>
    IThenCheckout IsShown();

    IThenCheckout ShowsTheCart();

    /// <summary>The checkout's order review has exactly these lines, in this order.</summary>
    IThenCheckout HasReviewLines(params string[] skuAliases);

    IThenCheckout HasReviewLine(string skuAlias, int expectedQuantity, string expectedLineTotal);

    IThenCheckout HasReviewQuantity(string skuAlias, int expectedQuantity);

    IThenCheckout HasReviewSubtotal(string expectedSubtotal);

    /// <summary>The order review's tax country and estimated tax rate (e.g. "19%") when the checkout was opened.</summary>
    IThenCheckout HadTaxWhenOpened(string expectedCountry, string expectedRate);

    /// <summary>The order review's tax country and estimated tax rate (e.g. "19%") as the checkout ended.</summary>
    IThenCheckout HasTax(string expectedCountry, string expectedRate);

    IThenCheckout SaysDiscountAndTaxAreWorkedOutPerOrder();

    /// <summary>The saved address, as the checkout shows it, is selected.</summary>
    IThenCheckout HasSavedAddressSelected(string expectedAddress);

    IThenCheckout ShowsMessage(string expectedMessage);

    /// <summary>One of the lines under the checkout's error message, such as "City is required".</summary>
    IThenCheckout ShowsMessageDetail(string expectedDetail);

    /// <summary>The checkout tells the customer this line was rejected ("SKU: reason" under its message).</summary>
    IThenCheckout SaysRejected(string skuAlias, string expectedReason);

    IThenCheckout ThanksForTheOrder();

    /// <summary>The confirmation lists exactly one order for each of these products, in this order.</summary>
    IThenCheckout ConfirmsOrdersFor(params string[] skuAliases);

    IThenCheckout ConfirmsNoOrder();

    IThenCheckout LinksEachOrderToItsDetails();

    IThenCheckout OffersViewMyOrdersAndContinueShopping();

    /// <summary>The confirmation shows this line was rejected, for this reason.</summary>
    IThenCheckout ConfirmsRejected(string skuAlias, string expectedReason);

    IThenCheckout ConfirmsNothingRejected();

    /// <summary>After the order was placed, the cart has exactly these lines, in this order; none for an empty cart.</summary>
    IThenCheckout HasCartLines(params string[] skuAliases);

    IThenCheckout HasCartLineQuantity(string skuAlias, int expectedQuantity);

    TaskAwaiter GetAwaiter();
}
