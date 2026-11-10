using System.Runtime.CompilerServices;

namespace Dsl.Port.Then.Steps;

/// <summary>The catalog the executed browse showed. SKUs are aliases; titles, brands, prices and labels are literals as shown.</summary>
public interface IThenCatalog
{
    /// <summary>Exactly these products, in this order.</summary>
    IThenCatalog ShowsProducts(params string[] skuAliases);

    IThenCatalog ShowsNoProducts();

    IThenCatalog HasProductCount(int expectedCount);

    IThenCatalog ShowsProduct(string skuAlias, string expectedTitle, string expectedBrand, string expectedPrice);

    /// <summary>The category filter's options, "All" included, in order.</summary>
    IThenCatalog OffersCategories(params string[] expectedCategories);

    IThenCatalog HasCategorySelected(string expectedCategory);

    IThenCatalog OffersBrands(params string[] expectedBrands);

    IThenCatalog HasNoBrandTicked();

    IThenCatalog HasSort(string expectedSort);

    IThenCatalog ShowsMessage(string expectedMessage);

    IThenCatalog OffersToClearFilters();

    /// <summary>The rest of the site keeps working: the acting user can still open their order history.</summary>
    IThenCatalog CanOpenMyOrders();

    TaskAwaiter GetAwaiter();
}
