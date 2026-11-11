using System.Runtime.CompilerServices;
using Dsl.Port.Then.Steps;
using Dsl.Core.Shared;
using Dsl.Core.UseCase.UseCases;
using Driver.Port.Dtos;

namespace Dsl.Core.Scenario.Then;

/// <summary>
/// The confirmation shows order numbers only; which product each order is for is looked up on the order itself, the
/// way the confirmation's links lead to it.
/// </summary>
public class ThenSuccessCheckout<TSuccessResponse, TSuccessVerification> : IThenCheckout
    where TSuccessVerification : ResponseVerification<TSuccessResponse>
{
    private const string ReviewNote = "Discount and tax are worked out per order when it is placed.";
    private const string ThanksText = "Thank you for your order!";

    private readonly ThenStage<TSuccessResponse, TSuccessVerification> _thenClause;
    private readonly List<Func<CheckOutVerification, Task>> _verifications = [];

    internal ThenSuccessCheckout(ThenStage<TSuccessResponse, TSuccessVerification> thenClause)
    {
        _thenClause = thenClause;
    }

    /// <summary>The number of the order the checkout placed for this product.</summary>
    internal static async Task<string> OrderNumberForAsync(ThenStage<TSuccessResponse, TSuccessVerification> thenClause, string skuAlias)
    {
        var verification = await CheckoutVerificationAsync(thenClause);
        return verification.OrderNumberFor(await PlacedSkusAsync(thenClause, verification), skuAlias);
    }

    private static Task<List<KeyValuePair<string, string?>>> PlacedSkusAsync(
        ThenStage<TSuccessResponse, TSuccessVerification> thenClause, CheckOutVerification verification)
        => verification.PlacedSkusAsync(async orderNumber =>
        {
            var shop = await thenClause.App.MyShop(thenClause.Channel!);
            return (await shop.ViewOrder().OrderNumber(orderNumber).Execute()).ShouldSucceed().OrderedSku();
        });

    private static async Task<CheckOutVerification> CheckoutVerificationAsync(ThenStage<TSuccessResponse, TSuccessVerification> thenClause)
    {
        var result = await thenClause.GetExecutionResult();
        return result.Result.ShouldSucceed() as CheckOutVerification
            ?? throw new InvalidOperationException("Cannot verify the checkout: the executed operation was not a checkout");
    }

    private ThenSuccessCheckout<TSuccessResponse, TSuccessVerification> Add(Action<CheckOutVerification> verification)
    {
        _verifications.Add(v =>
        {
            verification(v);
            return Task.CompletedTask;
        });
        return this;
    }

    public ThenSuccessCheckout<TSuccessResponse, TSuccessVerification> IsShown()
        => Add(v => v.ShownPage(CheckOutResponse.Checkout));

    IThenCheckout IThenCheckout.IsShown() => IsShown();

    public ThenSuccessCheckout<TSuccessResponse, TSuccessVerification> ShowsTheCart()
        => Add(v => v.ShownPage(CheckOutResponse.Cart));

    IThenCheckout IThenCheckout.ShowsTheCart() => ShowsTheCart();

    public ThenSuccessCheckout<TSuccessResponse, TSuccessVerification> HasReviewLines(params string[] skuAliases)
        => Add(v => v.ReviewLines(skuAliases));

    IThenCheckout IThenCheckout.HasReviewLines(params string[] skuAliases) => HasReviewLines(skuAliases);

    public ThenSuccessCheckout<TSuccessResponse, TSuccessVerification> HasReviewLine(string skuAlias, int expectedQuantity, string expectedLineTotal)
        => Add(v => v.ReviewQuantity(skuAlias, expectedQuantity).ReviewLineTotal(skuAlias, expectedLineTotal));

    IThenCheckout IThenCheckout.HasReviewLine(string skuAlias, int expectedQuantity, string expectedLineTotal)
        => HasReviewLine(skuAlias, expectedQuantity, expectedLineTotal);

    public ThenSuccessCheckout<TSuccessResponse, TSuccessVerification> HasReviewQuantity(string skuAlias, int expectedQuantity)
        => Add(v => v.ReviewQuantity(skuAlias, expectedQuantity));

    IThenCheckout IThenCheckout.HasReviewQuantity(string skuAlias, int expectedQuantity) => HasReviewQuantity(skuAlias, expectedQuantity);

    public ThenSuccessCheckout<TSuccessResponse, TSuccessVerification> HasReviewSubtotal(string expectedSubtotal)
        => Add(v => v.ReviewSubtotal(expectedSubtotal));

    IThenCheckout IThenCheckout.HasReviewSubtotal(string expectedSubtotal) => HasReviewSubtotal(expectedSubtotal);

    public ThenSuccessCheckout<TSuccessResponse, TSuccessVerification> HadTaxWhenOpened(string expectedCountry, string expectedRate)
        => Add(v => v.OpenedTax(expectedCountry, expectedRate));

    IThenCheckout IThenCheckout.HadTaxWhenOpened(string expectedCountry, string expectedRate) => HadTaxWhenOpened(expectedCountry, expectedRate);

    public ThenSuccessCheckout<TSuccessResponse, TSuccessVerification> HasTax(string expectedCountry, string expectedRate)
        => Add(v => v.Tax(expectedCountry, expectedRate));

    IThenCheckout IThenCheckout.HasTax(string expectedCountry, string expectedRate) => HasTax(expectedCountry, expectedRate);

    public ThenSuccessCheckout<TSuccessResponse, TSuccessVerification> SaysDiscountAndTaxAreWorkedOutPerOrder()
        => Add(v => v.ReviewNote(ReviewNote));

    IThenCheckout IThenCheckout.SaysDiscountAndTaxAreWorkedOutPerOrder() => SaysDiscountAndTaxAreWorkedOutPerOrder();

    public ThenSuccessCheckout<TSuccessResponse, TSuccessVerification> HasSavedAddressSelected(string expectedAddress)
        => Add(v => v.SavedAddressSelected(expectedAddress));

    IThenCheckout IThenCheckout.HasSavedAddressSelected(string expectedAddress) => HasSavedAddressSelected(expectedAddress);

    public ThenSuccessCheckout<TSuccessResponse, TSuccessVerification> ShowsMessage(string expectedMessage)
        => Add(v => v.Message(expectedMessage));

    IThenCheckout IThenCheckout.ShowsMessage(string expectedMessage) => ShowsMessage(expectedMessage);

    public ThenSuccessCheckout<TSuccessResponse, TSuccessVerification> ShowsMessageDetail(string expectedDetail)
        => Add(v => v.MessageDetail(expectedDetail));

    IThenCheckout IThenCheckout.ShowsMessageDetail(string expectedDetail) => ShowsMessageDetail(expectedDetail);

    public ThenSuccessCheckout<TSuccessResponse, TSuccessVerification> SaysRejected(string skuAlias, string expectedReason)
        => Add(v => v.MessageDetail(skuAlias + ": " + expectedReason));

    IThenCheckout IThenCheckout.SaysRejected(string skuAlias, string expectedReason) => SaysRejected(skuAlias, expectedReason);

    public ThenSuccessCheckout<TSuccessResponse, TSuccessVerification> ThanksForTheOrder()
        => Add(v => v.Thanks(ThanksText));

    IThenCheckout IThenCheckout.ThanksForTheOrder() => ThanksForTheOrder();

    public ThenSuccessCheckout<TSuccessResponse, TSuccessVerification> ConfirmsOrdersFor(params string[] skuAliases)
    {
        _verifications.Add(async v =>
        {
            v.ShownPage(CheckOutResponse.Confirmation);
            v.PlacedFor(await PlacedSkusAsync(_thenClause, v), skuAliases);
        });
        return this;
    }

    IThenCheckout IThenCheckout.ConfirmsOrdersFor(params string[] skuAliases) => ConfirmsOrdersFor(skuAliases);

    public ThenSuccessCheckout<TSuccessResponse, TSuccessVerification> ConfirmsNoOrder()
        => Add(v => v.NoOrderConfirmed());

    IThenCheckout IThenCheckout.ConfirmsNoOrder() => ConfirmsNoOrder();

    public ThenSuccessCheckout<TSuccessResponse, TSuccessVerification> LinksEachOrderToItsDetails()
        => Add(v => v.EachOrderLinksToItsDetails());

    IThenCheckout IThenCheckout.LinksEachOrderToItsDetails() => LinksEachOrderToItsDetails();

    public ThenSuccessCheckout<TSuccessResponse, TSuccessVerification> OffersViewMyOrdersAndContinueShopping()
        => Add(v => v.ViewMyOrdersAndContinueShoppingOffered());

    IThenCheckout IThenCheckout.OffersViewMyOrdersAndContinueShopping() => OffersViewMyOrdersAndContinueShopping();

    public ThenSuccessCheckout<TSuccessResponse, TSuccessVerification> ConfirmsRejected(string skuAlias, string expectedReason)
        => Add(v => v.RejectedLine(skuAlias, expectedReason));

    IThenCheckout IThenCheckout.ConfirmsRejected(string skuAlias, string expectedReason) => ConfirmsRejected(skuAlias, expectedReason);

    public ThenSuccessCheckout<TSuccessResponse, TSuccessVerification> ConfirmsNothingRejected()
        => Add(v => v.NoRejectedLines());

    IThenCheckout IThenCheckout.ConfirmsNothingRejected() => ConfirmsNothingRejected();

    public ThenSuccessCheckout<TSuccessResponse, TSuccessVerification> HasCartLines(params string[] skuAliases)
        => Add(v => v.CartLines(skuAliases));

    IThenCheckout IThenCheckout.HasCartLines(params string[] skuAliases) => HasCartLines(skuAliases);

    public ThenSuccessCheckout<TSuccessResponse, TSuccessVerification> HasCartLineQuantity(string skuAlias, int expectedQuantity)
        => Add(v => v.CartLineQuantity(skuAlias, expectedQuantity));

    IThenCheckout IThenCheckout.HasCartLineQuantity(string skuAlias, int expectedQuantity) => HasCartLineQuantity(skuAlias, expectedQuantity);

    public TaskAwaiter GetAwaiter() => Execute().GetAwaiter();

    private async Task Execute()
    {
        var verification = await CheckoutVerificationAsync(_thenClause);
        foreach (var v in _verifications)
        {
            await v(verification);
        }
    }
}
