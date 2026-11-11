using System.Runtime.CompilerServices;
using Dsl.Port;
using Dsl.Port.Then.Steps;
using Dsl.Core.Shared;
using Dsl.Core.UseCase.UseCases;

namespace Dsl.Core.Scenario.Then;

public class ThenSuccessProductPage<TSuccessResponse, TSuccessVerification> : IThenProductPage
    where TSuccessVerification : ResponseVerification<TSuccessResponse>
{
    private readonly ThenStage<TSuccessResponse, TSuccessVerification> _thenClause;
    private readonly List<Func<ViewProductVerification, Task>> _verifications = [];

    internal ThenSuccessProductPage(ThenStage<TSuccessResponse, TSuccessVerification> thenClause)
    {
        _thenClause = thenClause;
    }

    private ThenSuccessProductPage<TSuccessResponse, TSuccessVerification> Add(Action<ViewProductVerification> verification)
    {
        _verifications.Add(v =>
        {
            verification(v);
            return Task.CompletedTask;
        });
        return this;
    }

    public ThenSuccessProductPage<TSuccessResponse, TSuccessVerification> HasTitle(string expectedTitle)
        => Add(v => v.Title(expectedTitle));

    IThenProductPage IThenProductPage.HasTitle(string expectedTitle) => HasTitle(expectedTitle);

    public ThenSuccessProductPage<TSuccessResponse, TSuccessVerification> HasBrand(string expectedBrand)
        => Add(v => v.Brand(expectedBrand));

    IThenProductPage IThenProductPage.HasBrand(string expectedBrand) => HasBrand(expectedBrand);

    public ThenSuccessProductPage<TSuccessResponse, TSuccessVerification> HasCategory(string expectedCategory)
        => Add(v => v.Category(expectedCategory));

    IThenProductPage IThenProductPage.HasCategory(string expectedCategory) => HasCategory(expectedCategory);

    public ThenSuccessProductPage<TSuccessResponse, TSuccessVerification> HasSku(string expectedSkuAlias)
        => Add(v => v.Sku(expectedSkuAlias));

    IThenProductPage IThenProductPage.HasSku(string expectedSkuAlias) => HasSku(expectedSkuAlias);

    public ThenSuccessProductPage<TSuccessResponse, TSuccessVerification> HasPrice(string expectedPrice)
        => Add(v => v.Price(expectedPrice));

    IThenProductPage IThenProductPage.HasPrice(string expectedPrice) => HasPrice(expectedPrice);

    public ThenSuccessProductPage<TSuccessResponse, TSuccessVerification> HasDescription(string expectedDescription)
        => Add(v => v.Description(expectedDescription));

    IThenProductPage IThenProductPage.HasDescription(string expectedDescription) => HasDescription(expectedDescription);

    public ThenSuccessProductPage<TSuccessResponse, TSuccessVerification> HasBreadcrumbs(params string[] expectedBreadcrumbs)
        => Add(v => v.Breadcrumbs(expectedBreadcrumbs));

    IThenProductPage IThenProductPage.HasBreadcrumbs(params string[] expectedBreadcrumbs) => HasBreadcrumbs(expectedBreadcrumbs);

    public ThenSuccessProductPage<TSuccessResponse, TSuccessVerification> HasQuantity(int expectedQuantity)
        => Add(v => v.Quantity(expectedQuantity));

    IThenProductPage IThenProductPage.HasQuantity(int expectedQuantity) => HasQuantity(expectedQuantity);

    public ThenSuccessProductPage<TSuccessResponse, TSuccessVerification> ShowsMessage(string expectedMessage)
        => Add(v => v.Message(expectedMessage));

    IThenProductPage IThenProductPage.ShowsMessage(string expectedMessage) => ShowsMessage(expectedMessage);

    public ThenSuccessProductPage<TSuccessResponse, TSuccessVerification> OffersBackToCatalog()
        => Add(v => v.BackToCatalogOffered());

    IThenProductPage IThenProductPage.OffersBackToCatalog() => OffersBackToCatalog();

    public ThenSuccessProductPage<TSuccessResponse, TSuccessVerification> DoesNotOfferAddToCart()
        => Add(v => v.AddToCartNotOffered());

    IThenProductPage IThenProductPage.DoesNotOfferAddToCart() => DoesNotOfferAddToCart();

    public ThenSuccessProductPage<TSuccessResponse, TSuccessVerification> HasCartBadge(int expectedCount)
        => Add(v => v.CartItemCount(expectedCount));

    IThenProductPage IThenProductPage.HasCartBadge(int expectedCount) => HasCartBadge(expectedCount);

    public ThenSuccessProductPage<TSuccessResponse, TSuccessVerification> IsOnPageOfProduct(string skuAlias)
        => Add(v => v.OnPageOfProduct(skuAlias));

    IThenProductPage IThenProductPage.IsOnPageOfProduct(string skuAlias) => IsOnPageOfProduct(skuAlias);

    public ThenSuccessProductPage<TSuccessResponse, TSuccessVerification> CartContains(string skuAlias, int expectedQuantity)
        => Add(v => v.CartContains(skuAlias, expectedQuantity));

    IThenProductPage IThenProductPage.CartContains(string skuAlias, int expectedQuantity) => CartContains(skuAlias, expectedQuantity);

    public ThenSuccessProductPage<TSuccessResponse, TSuccessVerification> CartIsEmpty()
        => Add(v => v.CartIsEmpty());

    IThenProductPage IThenProductPage.CartIsEmpty() => CartIsEmpty();

    public ThenSuccessProductPage<TSuccessResponse, TSuccessVerification> CatalogShowsProducts(params string[] skuAliases)
        => Add(v => v.CatalogShowsProducts(skuAliases));

    IThenProductPage IThenProductPage.CatalogShowsProducts(params string[] skuAliases) => CatalogShowsProducts(skuAliases);

    /// <summary>Opens the acting user's order history on the same channel, after the product page checks.</summary>
    public ThenSuccessProductPage<TSuccessResponse, TSuccessVerification> CanOpenMyOrders()
    {
        _verifications.Add(async _ =>
        {
            var shop = await _thenClause.App.MyShop(ChannelMode.Dynamic, _thenClause.Channel!);
            (await shop.BrowseOrderHistory().Execute()).ShouldSucceed();
        });
        return this;
    }

    IThenProductPage IThenProductPage.CanOpenMyOrders() => CanOpenMyOrders();

    public TaskAwaiter GetAwaiter() => Execute().GetAwaiter();

    private async Task Execute()
    {
        var result = await _thenClause.GetExecutionResult();
        if (result.Result.ShouldSucceed() is not ViewProductVerification verification)
            throw new InvalidOperationException("Cannot verify the product page: the executed operation did not view a product");

        foreach (var v in _verifications)
        {
            await v(verification);
        }
    }
}
