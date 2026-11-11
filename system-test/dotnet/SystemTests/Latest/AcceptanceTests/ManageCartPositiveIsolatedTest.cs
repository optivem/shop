using SystemTests.Latest.AcceptanceTests.Base;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

/// <summary>
/// Isolated: stages the shared ERP stub's product list, which every cart reads.
/// The cart lives in the browser (server session, cookie or local storage) and only the UI shows it; there is no cart API.
/// </summary>
[Collection("Isolated")]
[Trait("Category", "isolated")]
public class ManageCartPositiveIsolatedTest : BaseManageCartTest
{
    private const string AddedToCart = "Added to cart";

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-008.1")]
    public async Task ShouldShowTheCartsTotalQuantityInTheHeader(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().ManageCart()
            .WithCartContaining(Hp15, 2)
            .WithCartContaining(DellXps, 1)
            .WithCatalogBrowsed()
            .Then().ShouldSucceed()
            .Cart()
            .HasCartBadge(3);
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-008.1")]
    public async Task ShouldHideTheCartBadgeWhenTheCartIsEmpty(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().ManageCart()
            .WithCatalogBrowsed()
            .Then().ShouldSucceed()
            .Cart()
            .HasNoCartBadge();
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-008.2")]
    public async Task ShouldAddQuantity1FromTheCatalog(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().ManageCart()
            .WithAddedFromCatalog(LogiMx3S)
            .Then().ShouldSucceed()
            .Cart()
            .ShowsMessage(AddedToCart)
            .HasLines(LogiMx3S)
            .HasLineQuantity(LogiMx3S, 1)
            .HasCartBadge(1);
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-008.2")]
    public async Task ShouldRaiseTheQuantityOfAProductAlreadyInTheCart(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().ManageCart()
            .WithCartContaining(LogiMx3S, 1)
            .WithAddedFromCatalog(LogiMx3S)
            .Then().ShouldSucceed()
            .Cart()
            .HasLines(LogiMx3S)
            .HasLineQuantity(LogiMx3S, 2)
            .HasItemCount(1);
    }

    public static IEnumerable<object[]> CapCases()
    {
        yield return new object[] { 97, "2", 99 };
        yield return new object[] { 98, "5", 99 };
        yield return new object[] { 99, "1", 99 };
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [ChannelMemberData(nameof(CapCases))]
    [Trait("tag", "UC-008.3")]
    public async Task ShouldCapTheQuantityOfALineAt99WhenAdding(Channel channel, int before, string added, int after)
    {
        await GivenTheCatalog(channel)
            .When().ManageCart()
            .WithCartContaining(Hp15, before)
            .WithAddedFromProductPage(Hp15, added)
            .Then().ShouldSucceed()
            .Cart()
            .HasLines(Hp15)
            .HasLineQuantity(Hp15, after);
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-008.4")]
    public async Task ShouldShowEachCartLineWithItsTitleUnitPriceQuantityAndLineTotal(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().ManageCart()
            .WithCartContaining(Hp15, 2)
            .WithCartContaining(DellXps, 1)
            .Then().ShouldSucceed()
            .Cart()
            .HasLines(Hp15, DellXps)
            .HasLine(Hp15, "HP Laptop 15", "$699.99", 2, "$1,399.98")
            .HasLine(DellXps, "Dell XPS 13", "$1,299.99", 1, "$1,299.99")
            .HasItemCount(2);
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-008.4")]
    public async Task ShouldListTheLinesInTheOrderTheirProductsWereFirstAdded(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().ManageCart()
            .WithCartContaining(Lg27Ul500, 1)
            .WithCartContaining(Hp15, 1)
            .WithCartContaining(Lg27Ul500, 1)
            .Then().ShouldSucceed()
            .Cart()
            .HasLines(Lg27Ul500, Hp15);
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-008.5")]
    public async Task ShouldShowTheSubtotalAndLeaveTaxAndDiscountsToCheckout(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().ManageCart()
            .WithCartContaining(Hp15, 2)
            .WithCartContaining(DellXps, 1)
            .Then().ShouldSucceed()
            .Cart()
            .HasSubtotal("$2,699.97")
            .SaysTaxAndDiscountsAreCalculatedAtCheckout();
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-008.5")]
    public async Task ShouldShowTheCurrentCatalogPrice(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().ManageCart()
            .WithCartContaining(Hp15, 2)
            .WithErpPriceChanged(Hp15, "649.99")
            .Then().ShouldSucceed()
            .Cart()
            .HasLinePrices(Hp15, "$649.99", "$1,299.98")
            .HasSubtotal("$1,299.98");
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-008.6")]
    public async Task ShouldUpdateTheLineTotalSubtotalAndBadgeWhenTheQuantityChanges(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().ManageCart()
            .WithCartContaining(Hp15, 2)
            .WithCartContaining(DellXps, 1)
            .WithQuantityChanged(Hp15, "3")
            .Then().ShouldSucceed()
            .Cart()
            .HasLineQuantity(Hp15, 3)
            .HasLineTotal(Hp15, "$2,099.97")
            .HasSubtotal("$3,399.96")
            .HasCartBadge(4);
    }

    public static IEnumerable<object[]> StepperCases()
    {
        yield return new object[] { 1, true, 2 };
        yield return new object[] { 2, false, 1 };
        yield return new object[] { 1, false, 1 };
        yield return new object[] { 99, true, 99 };
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [ChannelMemberData(nameof(StepperCases))]
    [Trait("tag", "UC-008.6")]
    public async Task ShouldKeepTheQuantityBetween1And99WhenUsingTheStepper(Channel channel, int before, bool increases, int after)
    {
        var manageCart = GivenTheCatalog(channel)
            .When().ManageCart()
            .WithCartContaining(Hp15, before);
        manageCart = increases ? manageCart.WithQuantityIncreased(Hp15) : manageCart.WithQuantityDecreased(Hp15);
        await manageCart
            .Then().ShouldSucceed()
            .Cart()
            .HasLineQuantity(Hp15, after);
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-008.7")]
    public async Task ShouldRemoveALineFromTheCart(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().ManageCart()
            .WithCartContaining(Hp15, 2)
            .WithCartContaining(DellXps, 1)
            .WithLineRemoved(Hp15)
            .Then().ShouldSucceed()
            .Cart()
            .HasLines(DellXps)
            .HasSubtotal("$1,299.99")
            .HasCartBadge(1);
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-008.8")]
    public async Task ShouldKeepTheCartWithoutSigningIn(Channel channel)
    {
        await GivenTheCatalog(channel)
            .And().NotLoggedIn()
            .When().ManageCart()
            .WithAddedFromCatalog(Hp15)
            .Then().ShouldSucceed()
            .Cart()
            .DidNotAskToSignIn()
            .HasLines(Hp15);
    }

    // The monoliths keep the cart in the server session or a cookie, React in local storage: a visit later in the
    // same browser keeps all of them.
    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-008.8")]
    public async Task ShouldKeepTheCartAcrossVisits(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().ManageCart()
            .WithCartContaining(Hp15, 2)
            .WithSiteLeftAndRevisited()
            .Then().ShouldSucceed()
            .Cart()
            .HasLines(Hp15)
            .HasLineQuantity(Hp15, 2);
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-008.8")]
    public async Task ShouldKeepTheCartWhenTheVisitorSignsIn(Channel channel)
    {
        await GivenTheCatalog(channel)
            .And().NotLoggedIn()
            .When().ManageCart()
            .WithCartContaining(Hp15, 2)
            .WithCartContaining(DellXps, 1)
            .WithSignedInAsCustomer()
            .Then().ShouldSucceed()
            .Cart()
            .HasLines(Hp15, DellXps)
            .HasLineQuantity(Hp15, 2)
            .HasLineQuantity(DellXps, 1)
            .HasCartBadge(3);
    }
}
