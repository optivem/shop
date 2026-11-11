using SystemTests.Latest.AcceptanceTests.Base;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

/// <summary>
/// Isolated: stages the shared ERP stub's product list and takes the shared ERP stub down.
/// The messages, the link back to the catalog, the cart and the working site are what the UI shows; the API has none of them.
/// </summary>
[Collection("Isolated")]
[Trait("Category", "isolated")]
public class ViewProductNegativeIsolatedTest : BaseViewProductTest
{
    private const string ProductNotFound = "Product not found";
    private const string ProductsUnavailable = "Products are temporarily unavailable.";
    private const string QuantityError = "Quantity must be a whole number from 1 to 99.";

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-007.5")]
    [Trait("tag", "narrative-2a")]
    public async Task ShouldShowProductNotFoundForAnUnknownSku(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().ViewProduct().WithSku(NoSuchSku)
            .Then().ShouldSucceed()
            .ProductPage()
            .ShowsMessage(ProductNotFound)
            .OffersBackToCatalog();
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-007.5")]
    [Trait("tag", "narrative-2a")]
    public async Task ShouldGoBackToTheCatalogFromProductNotFound(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().ViewProduct().WithSku(NoSuchSku).WithBackToCatalogFollowed()
            .Then().ShouldSucceed()
            .ProductPage()
            .CatalogShowsProducts(AllProducts);
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-007.6")]
    [Trait("tag", "narrative-2b")]
    public async Task ShouldTellTheVisitorWhenTheErpIsUnavailable(Channel channel)
    {
        await GivenTheCatalog(channel)
            .And().Erp().Unavailable()
            .When().ViewProduct().WithSku(Hp15)
            .Then().ShouldSucceed()
            .ProductPage()
            .ShowsMessage(ProductsUnavailable)
            .DoesNotOfferAddToCart();
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-007.6")]
    [Trait("tag", "narrative-2b")]
    public async Task ShouldKeepTheRestOfTheSiteWorkingWhenTheErpIsUnavailable(Channel channel)
    {
        await GivenTheCatalog(channel)
            .And().LoggedInAsCustomer()
            .Erp().Unavailable()
            .When().ViewProduct().WithSku(Hp15)
            .Then().ShouldSucceed()
            .ProductPage()
            .ShowsMessage(ProductsUnavailable)
            .CanOpenMyOrders();
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [ChannelInlineData("0")]
    [ChannelInlineData("-1")]
    [ChannelInlineData("1.5")]
    [ChannelInlineData("abc")]
    [ChannelInlineData("100")]
    [Trait("tag", "UC-007.7")]
    [Trait("tag", "narrative-5a")]
    public async Task ShouldNotAddAQuantityThatIsNotAWholeNumberFrom1To99(Channel channel, string quantity)
    {
        await GivenTheCatalog(channel)
            .When().ViewProduct().WithSku(Hp15).WithAddedToCart(quantity)
            .Then().ShouldSucceed()
            .ProductPage()
            .ShowsMessage(QuantityError)
            .CartIsEmpty();
    }
}
