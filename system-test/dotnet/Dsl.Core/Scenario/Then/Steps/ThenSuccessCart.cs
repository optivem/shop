using System.Runtime.CompilerServices;
using Dsl.Port.Then.Steps;
using Dsl.Core.Shared;
using Dsl.Core.UseCase.UseCases;

namespace Dsl.Core.Scenario.Then;

public class ThenSuccessCart<TSuccessResponse, TSuccessVerification> : IThenCart
    where TSuccessVerification : ResponseVerification<TSuccessResponse>
{
    private const string CheckoutNote = "Tax and discounts are calculated at checkout.";

    private readonly ThenStage<TSuccessResponse, TSuccessVerification> _thenClause;
    private readonly List<Action<ManageCartVerification>> _verifications = [];

    internal ThenSuccessCart(ThenStage<TSuccessResponse, TSuccessVerification> thenClause)
    {
        _thenClause = thenClause;
    }

    private ThenSuccessCart<TSuccessResponse, TSuccessVerification> Add(Action<ManageCartVerification> verification)
    {
        _verifications.Add(verification);
        return this;
    }

    public ThenSuccessCart<TSuccessResponse, TSuccessVerification> ShowsMessage(string expectedMessage)
        => Add(v => v.Message(expectedMessage));

    IThenCart IThenCart.ShowsMessage(string expectedMessage) => ShowsMessage(expectedMessage);

    public ThenSuccessCart<TSuccessResponse, TSuccessVerification> HasCartBadge(int expectedCount)
        => Add(v => v.CartItemCount(expectedCount));

    IThenCart IThenCart.HasCartBadge(int expectedCount) => HasCartBadge(expectedCount);

    public ThenSuccessCart<TSuccessResponse, TSuccessVerification> HasNoCartBadge()
        => Add(v => v.NoCartBadge());

    IThenCart IThenCart.HasNoCartBadge() => HasNoCartBadge();

    public ThenSuccessCart<TSuccessResponse, TSuccessVerification> HasLines(params string[] skuAliases)
        => Add(v => v.Lines(skuAliases));

    IThenCart IThenCart.HasLines(params string[] skuAliases) => HasLines(skuAliases);

    public ThenSuccessCart<TSuccessResponse, TSuccessVerification> HasLine(string skuAlias, string expectedTitle,
        string expectedUnitPrice, int expectedQuantity, string expectedLineTotal)
        => Add(v => v.Line(skuAlias, expectedTitle, expectedUnitPrice, expectedQuantity, expectedLineTotal));

    IThenCart IThenCart.HasLine(string skuAlias, string expectedTitle, string expectedUnitPrice, int expectedQuantity,
        string expectedLineTotal)
        => HasLine(skuAlias, expectedTitle, expectedUnitPrice, expectedQuantity, expectedLineTotal);

    public ThenSuccessCart<TSuccessResponse, TSuccessVerification> HasLineQuantity(string skuAlias, int expectedQuantity)
        => Add(v => v.LineQuantity(skuAlias, expectedQuantity));

    IThenCart IThenCart.HasLineQuantity(string skuAlias, int expectedQuantity) => HasLineQuantity(skuAlias, expectedQuantity);

    public ThenSuccessCart<TSuccessResponse, TSuccessVerification> HasLineTotal(string skuAlias, string expectedLineTotal)
        => Add(v => v.LineTotal(skuAlias, expectedLineTotal));

    IThenCart IThenCart.HasLineTotal(string skuAlias, string expectedLineTotal) => HasLineTotal(skuAlias, expectedLineTotal);

    public ThenSuccessCart<TSuccessResponse, TSuccessVerification> HasLinePrices(string skuAlias, string expectedUnitPrice,
        string expectedLineTotal)
        => Add(v => v.LinePrices(skuAlias, expectedUnitPrice, expectedLineTotal));

    IThenCart IThenCart.HasLinePrices(string skuAlias, string expectedUnitPrice, string expectedLineTotal)
        => HasLinePrices(skuAlias, expectedUnitPrice, expectedLineTotal);

    public ThenSuccessCart<TSuccessResponse, TSuccessVerification> HasItemCount(int expectedCount)
        => Add(v => v.ItemCount(expectedCount));

    IThenCart IThenCart.HasItemCount(int expectedCount) => HasItemCount(expectedCount);

    public ThenSuccessCart<TSuccessResponse, TSuccessVerification> HasSubtotal(string expectedSubtotal)
        => Add(v => v.Subtotal(expectedSubtotal));

    IThenCart IThenCart.HasSubtotal(string expectedSubtotal) => HasSubtotal(expectedSubtotal);

    public ThenSuccessCart<TSuccessResponse, TSuccessVerification> SaysTaxAndDiscountsAreCalculatedAtCheckout()
        => Add(v => v.CheckoutNote(CheckoutNote));

    IThenCart IThenCart.SaysTaxAndDiscountsAreCalculatedAtCheckout() => SaysTaxAndDiscountsAreCalculatedAtCheckout();

    public ThenSuccessCart<TSuccessResponse, TSuccessVerification> OffersStartShopping()
        => Add(v => v.StartShoppingOffered());

    IThenCart IThenCart.OffersStartShopping() => OffersStartShopping();

    public ThenSuccessCart<TSuccessResponse, TSuccessVerification> DidNotAskToSignIn()
        => Add(v => v.NotAskedToSignIn());

    IThenCart IThenCart.DidNotAskToSignIn() => DidNotAskToSignIn();

    public TaskAwaiter GetAwaiter() => Execute().GetAwaiter();

    private async Task Execute()
    {
        var result = await _thenClause.GetExecutionResult();
        if (result.Result.ShouldSucceed() is not ManageCartVerification verification)
            throw new InvalidOperationException("Cannot verify the cart: the executed operation did not manage the cart");

        foreach (var v in _verifications)
        {
            v(verification);
        }
    }
}
