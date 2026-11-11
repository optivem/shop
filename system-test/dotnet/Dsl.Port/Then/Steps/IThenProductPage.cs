using System.Runtime.CompilerServices;

namespace Dsl.Port.Then.Steps;

/// <summary>The product page the executed view showed. SKUs are aliases; titles, prices and messages are literals as shown.</summary>
public interface IThenProductPage
{
    IThenProductPage HasTitle(string expectedTitle);

    IThenProductPage HasBrand(string expectedBrand);

    IThenProductPage HasCategory(string expectedCategory);

    IThenProductPage HasSku(string expectedSkuAlias);

    IThenProductPage HasPrice(string expectedPrice);

    IThenProductPage HasDescription(string expectedDescription);

    /// <summary>The breadcrumb trail, in order.</summary>
    IThenProductPage HasBreadcrumbs(params string[] expectedBreadcrumbs);

    IThenProductPage HasQuantity(int expectedQuantity);

    IThenProductPage ShowsMessage(string expectedMessage);

    IThenProductPage OffersBackToCatalog();

    IThenProductPage DoesNotOfferAddToCart();

    IThenProductPage HasCartBadge(int expectedCount);

    IThenProductPage IsOnPageOfProduct(string skuAlias);

    /// <summary>The visitor's cart has exactly this line.</summary>
    IThenProductPage CartContains(string skuAlias, int expectedQuantity);

    IThenProductPage CartIsEmpty();

    /// <summary>After following the link back, the catalog shows exactly these products, in this order.</summary>
    IThenProductPage CatalogShowsProducts(params string[] skuAliases);

    /// <summary>The rest of the site keeps working: the acting user can still open their order history.</summary>
    IThenProductPage CanOpenMyOrders();

    TaskAwaiter GetAwaiter();
}
