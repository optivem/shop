using SystemTests.Latest.AcceptanceTests.Base;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

/// <summary>
/// Isolated: stages the shared ERP stub's product list and takes the shared ERP stub down.
/// The messages, the "Clear filters" offer and the working site are what the UI shows; the API has none of them.
/// </summary>
[Collection("Isolated")]
[Trait("Category", "isolated")]
public class BrowseCatalogNegativeIsolatedTest : BaseBrowseCatalogTest
{
    private const string NoMatch = "No products match your search.";
    private const string ProductsUnavailable = "Products are temporarily unavailable.";

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-006.9")]
    [Trait("tag", "narrative-5a")]
    public async Task ShouldTellTheVisitorWhenNoProductMatches(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().BrowseCatalog().WithSearch("printer")
            .Then().ShouldSucceed()
            .Catalog()
            .ShowsNoProducts()
            .ShowsMessage(NoMatch)
            .OffersToClearFilters();
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-006.9")]
    [Trait("tag", "narrative-5a")]
    public async Task ShouldShowTheFullCatalogAfterClearingTheFilters(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().BrowseCatalog().WithSearch("printer").WithFiltersCleared()
            .Then().ShouldSucceed()
            .Catalog()
            .ShowsProducts(AllProducts)
            .HasCategorySelected("All")
            .HasSort(Featured);
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-006.10")]
    [Trait("tag", "narrative-2a")]
    public async Task ShouldTellTheVisitorWhenTheErpIsUnavailable(Channel channel)
    {
        await GivenTheCatalog(channel)
            .And().Erp().Unavailable()
            .When().BrowseCatalog()
            .Then().ShouldSucceed()
            .Catalog()
            .ShowsNoProducts()
            .ShowsMessage(ProductsUnavailable);
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-006.10")]
    [Trait("tag", "narrative-2a")]
    public async Task ShouldKeepTheRestOfTheSiteWorkingWhenTheErpIsUnavailable(Channel channel)
    {
        await GivenTheCatalog(channel)
            .And().LoggedInAsCustomer()
            .Erp().Unavailable()
            .When().BrowseCatalog()
            .Then().ShouldSucceed()
            .Catalog()
            .ShowsMessage(ProductsUnavailable)
            .CanOpenMyOrders();
    }
}
