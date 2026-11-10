using SystemTests.Latest.AcceptanceTests.Base;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

/// <summary>
/// Isolated: stages the shared ERP stub's product list, which every catalog page reads.
/// Filtering, search and sort exist only in the UI; the API serves the unfiltered list.
/// </summary>
[Collection("Isolated")]
[Trait("Category", "isolated")]
public class BrowseCatalogPositiveIsolatedTest : BaseBrowseCatalogTest
{
    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-006.1")]
    public async Task ShouldListEveryErpProductInTheErpsOrder(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().BrowseCatalog()
            .Then().ShouldSucceed()
            .Catalog()
            .ShowsProducts(AllProducts)
            .HasProductCount(6);
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-006.1")]
    public async Task ShouldShowTheTitleBrandAndPriceOfEachProduct(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().BrowseCatalog()
            .Then().ShouldSucceed()
            .Catalog()
            .ShowsProduct(DellXps, "Dell XPS 13", "Dell", "$1,299.99")
            .ShowsProduct(LogiMx3S, "Logitech MX Master 3S", "Logitech", "$99.99");
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-006.2")]
    public async Task ShouldOfferTheCategoriesOfTheCatalogsProducts(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().BrowseCatalog()
            .Then().ShouldSucceed()
            .Catalog()
            .OffersCategories("All", "Laptops", "Monitors", "Accessories")
            .HasCategorySelected("All");
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-006.2")]
    public async Task ShouldShowOnlyTheProductsInTheChosenCategory(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().BrowseCatalog().WithCategory("Monitors")
            .Then().ShouldSucceed()
            .Catalog()
            .ShowsProducts(DellU2723Qe, Lg27Ul500)
            .HasProductCount(2);
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-006.2")]
    public async Task ShouldFilterByCategoryFromTheHeaderLink(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().BrowseCatalog().WithHeaderCategoryLink("Laptops")
            .Then().ShouldSucceed()
            .Catalog()
            .ShowsProducts(Hp15, DellXps, LenovoT14)
            .HasCategorySelected("Laptops");
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-006.3")]
    public async Task ShouldOfferTheBrandsOfTheCatalogsProducts(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().BrowseCatalog()
            .Then().ShouldSucceed()
            .Catalog()
            .OffersBrands("HP", "Dell", "Lenovo", "LG", "Logitech")
            .HasNoBrandTicked();
    }

    public static IEnumerable<object[]> BrandFilters()
    {
        yield return new object[] { new[] { "Dell" }, new[] { DellXps, DellU2723Qe } };
        yield return new object[] { new[] { "Dell", "LG" }, new[] { DellXps, DellU2723Qe, Lg27Ul500 } };
        yield return new object[] { Array.Empty<string>(), AllProducts };
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [ChannelMemberData(nameof(BrandFilters))]
    [Trait("tag", "UC-006.3")]
    public async Task ShouldShowOnlyTheProductsOfTheTickedBrands(Channel channel, string[] brands, string[] expectedProducts)
    {
        await GivenTheCatalog(channel)
            .When().BrowseCatalog().WithBrands(brands)
            .Then().ShouldSucceed()
            .Catalog()
            .ShowsProducts(expectedProducts);
    }

    public static IEnumerable<object[]> SearchTerms()
    {
        yield return new object[] { "thinkpad", new[] { LenovoT14 } };
        yield return new object[] { "LOGITECH", new[] { LogiMx3S } };
        yield return new object[] { "wireless", new[] { LogiMx3S } };
        yield return new object[] { "4k", new[] { DellU2723Qe, Lg27Ul500 } };
        yield return new object[] { "", AllProducts };
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [ChannelMemberData(nameof(SearchTerms))]
    [Trait("tag", "UC-006.4")]
    public async Task ShouldFindProductsByTitleBrandOrDescriptionIgnoringCase(Channel channel, string term, string[] expectedProducts)
    {
        await GivenTheCatalog(channel)
            .When().BrowseCatalog().WithSearch(term)
            .Then().ShouldSucceed()
            .Catalog()
            .ShowsProducts(expectedProducts);
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-006.5")]
    public async Task ShouldShowOnlyProductsThatMatchEveryFilter(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().BrowseCatalog().WithCategory("Laptops").WithBrands("Dell")
            .Then().ShouldSucceed()
            .Catalog()
            .ShowsProducts(DellXps)
            .HasProductCount(1);
    }

    public static IEnumerable<object[]> SortOrders()
    {
        yield return new object[] { Featured, AllProducts };
        yield return new object[] { PriceLowToHigh, new[] { LogiMx3S, Lg27Ul500, DellU2723Qe, Hp15, LenovoT14, DellXps } };
        yield return new object[] { PriceHighToLow, new[] { DellXps, LenovoT14, Hp15, DellU2723Qe, Lg27Ul500, LogiMx3S } };
        yield return new object[] { NameAToZ, new[] { DellU2723Qe, DellXps, Hp15, LenovoT14, Lg27Ul500, LogiMx3S } };
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [ChannelMemberData(nameof(SortOrders))]
    [Trait("tag", "UC-006.6")]
    public async Task ShouldSortTheProducts(Channel channel, string sort, string[] expectedProducts)
    {
        await GivenTheCatalog(channel)
            .When().BrowseCatalog().WithSort(sort)
            .Then().ShouldSucceed()
            .Catalog()
            .ShowsProducts(expectedProducts);
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-006.6")]
    public async Task ShouldSortOnlyTheFilteredProducts(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().BrowseCatalog().WithCategory("Laptops").WithSort(PriceHighToLow)
            .Then().ShouldSucceed()
            .Catalog()
            .ShowsProducts(DellXps, LenovoT14, Hp15);
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-006.7")]
    public async Task ShouldShowTheSameFilteredViewWhenThePageAddressIsOpenedAgain(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().BrowseCatalog().WithCategory("Laptops").WithSort(PriceHighToLow).WithPageAddressReopened()
            .Then().ShouldSucceed()
            .Catalog()
            .ShowsProducts(DellXps, LenovoT14, Hp15)
            .HasCategorySelected("Laptops")
            .HasSort(PriceHighToLow);
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-006.8")]
    public async Task ShouldLetAnAnonymousVisitorBrowseWithoutSigningIn(Channel channel)
    {
        await GivenTheCatalog(channel)
            .And().NotLoggedIn()
            .When().BrowseCatalog()
            .Then().ShouldSucceed()
            .Catalog()
            .HasProductCount(6);
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-006.8")]
    public async Task ShouldShowASignedInCustomerTheSameCatalog(Channel channel)
    {
        await GivenTheCatalog(channel)
            .And().LoggedInAsCustomer()
            .When().BrowseCatalog()
            .Then().ShouldSucceed()
            .Catalog()
            .ShowsProducts(AllProducts);
    }
}
