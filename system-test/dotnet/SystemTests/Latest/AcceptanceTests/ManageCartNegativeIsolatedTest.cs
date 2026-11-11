using SystemTests.Latest.AcceptanceTests.Base;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

/// <summary>
/// Isolated: stages the shared ERP stub's product list and takes the shared ERP stub down.
/// The cart lives in the browser and only the UI shows it and its messages; there is no cart API.
/// </summary>
[Collection("Isolated")]
[Trait("Category", "isolated")]
public class ManageCartNegativeIsolatedTest : BaseManageCartTest
{
    private const string CartIsEmpty = "Your cart is empty.";
    private const string ProductsUnavailable = "Products are temporarily unavailable.";
    private const string QuantityError = "Quantity must be a whole number from 1 to 99.";

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-008.9")]
    [Trait("tag", "narrative-4a")]
    public async Task ShouldTellTheVisitorWhenTheCartIsEmpty(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().ManageCart()
            .Then().ShouldSucceed()
            .Cart()
            .ShowsMessage(CartIsEmpty)
            .OffersStartShopping();
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-008.9")]
    [Trait("tag", "narrative-4a")]
    public async Task ShouldShowTheEmptyCartAfterRemovingTheLastLine(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().ManageCart()
            .WithCartContaining(Hp15, 1)
            .WithLineRemoved(Hp15)
            .Then().ShouldSucceed()
            .Cart()
            .ShowsMessage(CartIsEmpty)
            .HasNoCartBadge();
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-008.10")]
    [Trait("tag", "narrative-4b")]
    public async Task ShouldTellTheVisitorWhenTheErpIsUnavailableAndKeepTheCart(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().ManageCart()
            .WithCartContaining(Hp15, 2)
            .WithErpUnavailable()
            .Then().ShouldSucceed()
            .Cart()
            .ShowsMessage(ProductsUnavailable)
            .HasCartBadge(2);
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-008.10")]
    [Trait("tag", "narrative-4b")]
    public async Task ShouldShowTheCartAgainOnceTheErpIsBack(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().ManageCart()
            .WithCartContaining(Hp15, 2)
            .WithCartViewedWhileErpUnavailable()
            .Then().ShouldSucceed()
            .Cart()
            .HasLineQuantity(Hp15, 2)
            .HasLineTotal(Hp15, "$1,399.98");
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [ChannelInlineData("0")]
    [ChannelInlineData("-1")]
    [ChannelInlineData("1.5")]
    [ChannelInlineData("abc")]
    [ChannelInlineData("100")]
    [Trait("tag", "UC-008.11")]
    [Trait("tag", "narrative-5a")]
    public async Task ShouldKeepThePreviousQuantityWhenTheNewOneIsNotAWholeNumberFrom1To99(Channel channel, string quantity)
    {
        await GivenTheCatalog(channel)
            .When().ManageCart()
            .WithCartContaining(Hp15, 2)
            .WithQuantityChanged(Hp15, quantity)
            .Then().ShouldSucceed()
            .Cart()
            .ShowsMessage(QuantityError)
            .HasLineQuantity(Hp15, 2);
    }
}
