using SystemTests.Latest.AcceptanceTests.Base;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

/// <summary>
/// Isolated: stages the shared ERP stub's product list, which every product page reads.
/// The formatted price, breadcrumbs, quantity stepper and cart exist only in the UI; the API serves the product's fields.
/// </summary>
[Collection("Isolated")]
[Trait("Category", "isolated")]
public class ViewProductPositiveIsolatedTest : BaseViewProductTest
{
    private const string AddedToCart = "Added to cart";

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-007.1")]
    public async Task ShouldShowTheDetailsOfTheProduct(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().ViewProduct().WithSku(DellXps)
            .Then().ShouldSucceed()
            .ProductPage()
            .HasTitle("Dell XPS 13")
            .HasBrand("Dell")
            .HasCategory("Laptops")
            .HasSku(DellXps)
            .HasPrice("$1,299.99")
            .HasDescription("Compact 13-inch ultrabook");
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-007.1")]
    public async Task ShouldShowTheBreadcrumbTrailOfTheProduct(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().ViewProduct().WithSku(LogiMx3S)
            .Then().ShouldSucceed()
            .ProductPage()
            .HasBreadcrumbs("Home", "Accessories", "Logitech MX Master 3S");
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-007.1")]
    public async Task ShouldOpenTheProductFromTheCatalog(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().ViewProduct().WithSku(Hp15).FromCatalog()
            .Then().ShouldSucceed()
            .ProductPage()
            .HasTitle("HP Laptop 15")
            .HasPrice("$699.99");
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-007.2")]
    public async Task ShouldStartWithQuantity1(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().ViewProduct().WithSku(Hp15)
            .Then().ShouldSucceed()
            .ProductPage()
            .HasQuantity(1);
    }

    public static IEnumerable<object[]> StepperCases()
    {
        yield return new object[] { "1", true, 2 };
        yield return new object[] { "2", false, 1 };
        yield return new object[] { "1", false, 1 };
        yield return new object[] { "99", true, 99 };
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [ChannelMemberData(nameof(StepperCases))]
    [Trait("tag", "UC-007.2")]
    public async Task ShouldKeepTheQuantityBetween1And99WhenUsingTheStepper(Channel channel, string before, bool increases, int after)
    {
        var viewProduct = GivenTheCatalog(channel)
            .When().ViewProduct().WithSku(Hp15).WithQuantity(before);
        viewProduct = increases ? viewProduct.WithQuantityIncreased() : viewProduct.WithQuantityDecreased();
        await viewProduct
            .Then().ShouldSucceed()
            .ProductPage()
            .HasQuantity(after);
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-007.3")]
    public async Task ShouldAddTheChosenQuantityToTheCart(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().ViewProduct().WithSku(Hp15).WithAddedToCart("3")
            .Then().ShouldSucceed()
            .ProductPage()
            .ShowsMessage(AddedToCart)
            .CartContains(Hp15, 3)
            .HasCartBadge(3)
            .IsOnPageOfProduct(Hp15);
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-007.3")]
    public async Task ShouldAddToAProductAlreadyInTheCart(Channel channel)
    {
        await GivenTheCatalog(channel)
            .When().ViewProduct().WithCartContaining(Hp15, 1).WithSku(Hp15).WithAddedToCart("2")
            .Then().ShouldSucceed()
            .ProductPage()
            .CartContains(Hp15, 3);
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-007.4")]
    public async Task ShouldLetAnAnonymousVisitorViewAProductWithoutSigningIn(Channel channel)
    {
        await GivenTheCatalog(channel)
            .And().NotLoggedIn()
            .When().ViewProduct().WithSku(LenovoT14)
            .Then().ShouldSucceed()
            .ProductPage()
            .HasTitle("Lenovo ThinkPad T14");
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-007.4")]
    public async Task ShouldShowASignedInCustomerTheSameProductPage(Channel channel)
    {
        await GivenTheCatalog(channel)
            .And().LoggedInAsCustomer()
            .When().ViewProduct().WithSku(LenovoT14)
            .Then().ShouldSucceed()
            .ProductPage()
            .HasTitle("Lenovo ThinkPad T14")
            .HasBrand("Lenovo")
            .HasCategory("Laptops")
            .HasSku(LenovoT14)
            .HasPrice("$999.99");
    }
}
