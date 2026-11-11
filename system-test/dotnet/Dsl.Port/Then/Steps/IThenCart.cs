using System.Runtime.CompilerServices;

namespace Dsl.Port.Then.Steps;

/// <summary>The cart the visitor's action left. SKUs are aliases; titles, prices and messages are literals as shown.</summary>
public interface IThenCart
{
    IThenCart ShowsMessage(string expectedMessage);

    IThenCart HasCartBadge(int expectedCount);

    IThenCart HasNoCartBadge();

    /// <summary>The cart has exactly these lines, in this order; none for an empty cart.</summary>
    IThenCart HasLines(params string[] skuAliases);

    IThenCart HasLine(string skuAlias, string expectedTitle, string expectedUnitPrice, int expectedQuantity, string expectedLineTotal);

    IThenCart HasLineQuantity(string skuAlias, int expectedQuantity);

    IThenCart HasLineTotal(string skuAlias, string expectedLineTotal);

    IThenCart HasLinePrices(string skuAlias, string expectedUnitPrice, string expectedLineTotal);

    /// <summary>The number of items (lines) the cart shows.</summary>
    IThenCart HasItemCount(int expectedCount);

    IThenCart HasSubtotal(string expectedSubtotal);

    IThenCart SaysTaxAndDiscountsAreCalculatedAtCheckout();

    IThenCart OffersStartShopping();

    IThenCart DidNotAskToSignIn();

    TaskAwaiter GetAwaiter();
}
