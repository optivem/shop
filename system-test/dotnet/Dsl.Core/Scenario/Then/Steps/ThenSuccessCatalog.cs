using System.Runtime.CompilerServices;
using Dsl.Port;
using Dsl.Port.Then.Steps;
using Dsl.Core.Shared;
using Dsl.Core.UseCase.UseCases;

namespace Dsl.Core.Scenario.Then;

public class ThenSuccessCatalog<TSuccessResponse, TSuccessVerification> : IThenCatalog
    where TSuccessVerification : ResponseVerification<TSuccessResponse>
{
    private readonly ThenStage<TSuccessResponse, TSuccessVerification> _thenClause;
    private readonly List<Func<BrowseCatalogVerification, Task>> _verifications = [];

    internal ThenSuccessCatalog(ThenStage<TSuccessResponse, TSuccessVerification> thenClause)
    {
        _thenClause = thenClause;
    }

    private ThenSuccessCatalog<TSuccessResponse, TSuccessVerification> Add(Action<BrowseCatalogVerification> verification)
    {
        _verifications.Add(v =>
        {
            verification(v);
            return Task.CompletedTask;
        });
        return this;
    }

    public ThenSuccessCatalog<TSuccessResponse, TSuccessVerification> ShowsProducts(params string[] skuAliases)
        => Add(v => v.ShowsProducts(skuAliases));

    IThenCatalog IThenCatalog.ShowsProducts(params string[] skuAliases) => ShowsProducts(skuAliases);

    public ThenSuccessCatalog<TSuccessResponse, TSuccessVerification> ShowsNoProducts()
        => Add(v => v.ShowsNoProducts());

    IThenCatalog IThenCatalog.ShowsNoProducts() => ShowsNoProducts();

    public ThenSuccessCatalog<TSuccessResponse, TSuccessVerification> HasProductCount(int expectedCount)
        => Add(v => v.ProductCount(expectedCount));

    IThenCatalog IThenCatalog.HasProductCount(int expectedCount) => HasProductCount(expectedCount);

    public ThenSuccessCatalog<TSuccessResponse, TSuccessVerification> ShowsProduct(string skuAlias, string expectedTitle, string expectedBrand, string expectedPrice)
        => Add(v => v.Product(skuAlias, expectedTitle, expectedBrand, expectedPrice));

    IThenCatalog IThenCatalog.ShowsProduct(string skuAlias, string expectedTitle, string expectedBrand, string expectedPrice)
        => ShowsProduct(skuAlias, expectedTitle, expectedBrand, expectedPrice);

    public ThenSuccessCatalog<TSuccessResponse, TSuccessVerification> OffersCategories(params string[] expectedCategories)
        => Add(v => v.CategoryOptions(expectedCategories));

    IThenCatalog IThenCatalog.OffersCategories(params string[] expectedCategories) => OffersCategories(expectedCategories);

    public ThenSuccessCatalog<TSuccessResponse, TSuccessVerification> HasCategorySelected(string expectedCategory)
        => Add(v => v.SelectedCategory(expectedCategory));

    IThenCatalog IThenCatalog.HasCategorySelected(string expectedCategory) => HasCategorySelected(expectedCategory);

    public ThenSuccessCatalog<TSuccessResponse, TSuccessVerification> OffersBrands(params string[] expectedBrands)
        => Add(v => v.BrandOptions(expectedBrands));

    IThenCatalog IThenCatalog.OffersBrands(params string[] expectedBrands) => OffersBrands(expectedBrands);

    public ThenSuccessCatalog<TSuccessResponse, TSuccessVerification> HasNoBrandTicked()
        => Add(v => v.NoBrandTicked());

    IThenCatalog IThenCatalog.HasNoBrandTicked() => HasNoBrandTicked();

    public ThenSuccessCatalog<TSuccessResponse, TSuccessVerification> HasSort(string expectedSort)
        => Add(v => v.Sort(expectedSort));

    IThenCatalog IThenCatalog.HasSort(string expectedSort) => HasSort(expectedSort);

    public ThenSuccessCatalog<TSuccessResponse, TSuccessVerification> ShowsMessage(string expectedMessage)
        => Add(v => v.Message(expectedMessage));

    IThenCatalog IThenCatalog.ShowsMessage(string expectedMessage) => ShowsMessage(expectedMessage);

    public ThenSuccessCatalog<TSuccessResponse, TSuccessVerification> OffersToClearFilters()
        => Add(v => v.ClearFiltersOffered());

    IThenCatalog IThenCatalog.OffersToClearFilters() => OffersToClearFilters();

    /// <summary>Opens the acting user's order history on the same channel, after the catalog checks.</summary>
    public ThenSuccessCatalog<TSuccessResponse, TSuccessVerification> CanOpenMyOrders()
    {
        _verifications.Add(async _ =>
        {
            var shop = await _thenClause.App.MyShop(ChannelMode.Dynamic, _thenClause.Channel!);
            (await shop.BrowseOrderHistory().Execute()).ShouldSucceed();
        });
        return this;
    }

    IThenCatalog IThenCatalog.CanOpenMyOrders() => CanOpenMyOrders();

    public TaskAwaiter GetAwaiter() => Execute().GetAwaiter();

    private async Task Execute()
    {
        var result = await _thenClause.GetExecutionResult();
        if (result.Result.ShouldSucceed() is not BrowseCatalogVerification verification)
            throw new InvalidOperationException("Cannot verify the catalog: the executed operation did not browse the catalog");

        foreach (var v in _verifications)
        {
            await v(verification);
        }
    }
}
