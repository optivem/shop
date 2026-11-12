using SystemTests.Latest.AcceptanceTests.Base;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

/// <summary>
/// Isolated: stages the shared ERP stub's product list and saves the shared test customer's profile.
/// The cart lives in the browser and only the UI checks it out; there is no checkout API. The orders it places are
/// checked through the order API.
/// </summary>
[Collection("Isolated")]
[Trait("Category", "isolated")]
public class CheckOutPositiveIsolatedTest : BaseCheckOutTest
{
    private const string Summer2025 = "SUMMER2025";

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-009.1")]
    public async Task ShouldPlaceOneOrderPerCartLine(Channel channel)
    {
        var address = AnaNovakBerlin;
        var then = GivenTheCatalogAndSavedAddress(channel, address)
            .When().CheckOut()
            .WithCartContaining(Hp15, 2)
            .WithCartContaining(DellXps, 1)
            .WithOrderPlaced()
            .Then();

        await then.ShouldSucceed()
            .Checkout()
            .ConfirmsOrdersFor(Hp15, DellXps);

        await then.ShouldSucceed()
            .And().OrderFor(Hp15)
            .HasQuantity(2)
            .HasShippingAddressWithCountryCode(address.FirstName, address.LastName, address.Street,
                address.PostalCode, address.City, address.Country);

        await then.ShouldSucceed()
            .And().OrderFor(DellXps)
            .HasQuantity(1)
            .HasShippingAddressWithCountryCode(address.FirstName, address.LastName, address.Street,
                address.PostalCode, address.City, address.Country);
    }

    // The confirmation lists the orders in the order they were placed.
    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-009.1")]
    public async Task ShouldPlaceTheOrdersInCartOrder(Channel channel)
    {
        await GivenTheCatalogAndSavedAddress(channel, AnaNovakBerlin)
            .When().CheckOut()
            .WithCartContaining(LogiMx3S, 1)
            .WithCartContaining(Hp15, 1)
            .WithOrderPlaced()
            .Then().ShouldSucceed()
            .Checkout()
            .ConfirmsOrdersFor(LogiMx3S, Hp15);
    }

    public static IEnumerable<object[]> TaxCases()
    {
        yield return new object[] { JohnSmithNewYork, "US", "0.00" };
        yield return new object[] { AnaNovakBerlin, "DE", "0.19" };
        yield return new object[] { JaneBrownLondon, "GB", "0.20" };
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [ChannelMemberData(nameof(TaxCases))]
    [Trait("tag", "UC-009.2")]
    public async Task ShouldApplyTheTaxRateOfTheShippingAddressesCountry(Channel channel, Address address, string country, string taxRate)
    {
        await GivenTheCatalogAndSavedAddress(channel, address)
            .When().CheckOut()
            .WithCartContaining(LogiMx3S, 1)
            .WithOrderPlaced()
            .Then().ShouldSucceed()
            .And().OrderFor(LogiMx3S)
            .HasCountryCode(country)
            .HasTaxRate(taxRate);
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-009.2")]
    public async Task ShouldShowTheEstimatedTaxForTheChosenShippingCountry(Channel channel)
    {
        var other = JaneBrownLondon;
        await GivenTheCatalogAndSavedAddress(channel, AnaNovakBerlin)
            .When().CheckOut()
            .WithCartContaining(Hp15, 1)
            .WithShippingAddress(other.FirstName, other.LastName, other.Street, other.PostalCode, other.City, other.Country)
            .Then().ShouldSucceed()
            .Checkout()
            .HadTaxWhenOpened("DE", "19%")
            .HasTax("GB", "20%");
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-009.3")]
    [Trait("tag", "UC-010.5")]
    public async Task ShouldPreselectTheSavedShippingAddress(Channel channel)
    {
        await GivenTheCatalogAndSavedAddress(channel, AnaNovakBerlin)
            .When().CheckOut()
            .WithCartContaining(Hp15, 1)
            .Then().ShouldSucceed()
            .Checkout()
            .HasSavedAddressSelected(AnaNovakBerlin.Formatted());
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-009.3")]
    public async Task ShouldShipToADifferentAddressWithoutChangingTheProfile(Channel channel)
    {
        var saved = AnaNovakBerlin;
        var other = JaneBrownLondon;
        var then = GivenTheCatalogAndSavedAddress(channel, saved)
            .When().CheckOut()
            .WithCartContaining(Hp15, 1)
            .WithShippingAddress(other.FirstName, other.LastName, other.Street, other.PostalCode, other.City, other.Country)
            .WithOrderPlaced()
            .Then();

        await then.ShouldSucceed()
            .And().OrderFor(Hp15)
            .HasShippingAddressWithCountryCode(other.FirstName, other.LastName, other.Street,
                other.PostalCode, other.City, other.Country)
            .HasTaxRate("0.20");

        await then.ShouldSucceed()
            .And().Profile()
            .HasFirstName(saved.FirstName)
            .HasLastName(saved.LastName)
            .HasStreet(saved.Street)
            .HasPostalCode(saved.PostalCode)
            .HasCity(saved.City)
            .HasCountry(saved.Country);
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-009.4")]
    public async Task ShouldApplyTheCouponToEveryOrder(Channel channel)
    {
        var then = GivenTheCatalogAndSavedAddress(channel, JohnSmithNewYork)
            .Coupon()
            .WithCouponCode(Summer2025)
            .WithDiscountRate("0.15")
            .When().CheckOut()
            .WithCartContaining(Hp15, 1)
            .WithCartContaining(LogiMx3S, 1)
            .WithCouponCode(Summer2025)
            .WithOrderPlaced()
            .Then();

        await then.ShouldSucceed()
            .Checkout()
            .ConfirmsOrdersFor(Hp15, LogiMx3S);

        await then.ShouldSucceed()
            .And().OrderFor(Hp15)
            .HasAppliedCoupon(Summer2025)
            .HasDiscountRate(0.15m);

        await then.ShouldSucceed()
            .And().OrderFor(LogiMx3S)
            .HasAppliedCoupon(Summer2025)
            .HasDiscountRate(0.15m);

        await then.ShouldSucceed()
            .And().Coupon(Summer2025)
            .HasUsedCount(2);
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-009.4")]
    public async Task ShouldApplyNoDiscountWhenThereIsNoCoupon(Channel channel)
    {
        await GivenTheCatalogAndSavedAddress(channel, JohnSmithNewYork)
            .When().CheckOut()
            .WithCartContaining(Hp15, 1)
            .WithOrderPlaced()
            .Then().ShouldSucceed()
            .And().OrderFor(Hp15)
            .HasAppliedCoupon(null!)
            .HasDiscountAmount("0.00");
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-009.5")]
    public async Task ShouldReviewTheCartLinesAndSubtotalAtCatalogPrices(Channel channel)
    {
        await GivenTheCatalogAndSavedAddress(channel, AnaNovakBerlin)
            .When().CheckOut()
            .WithCartContaining(Hp15, 2)
            .WithCartContaining(DellXps, 1)
            .Then().ShouldSucceed()
            .Checkout()
            .HasReviewLines(Hp15, DellXps)
            .HasReviewLine(Hp15, 2, "$1,399.98")
            .HasReviewLine(DellXps, 1, "$1,299.99")
            .HasReviewSubtotal("$2,699.97")
            .SaysDiscountAndTaxAreWorkedOutPerOrder();
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-009.5")]
    public async Task ShouldPriceEachOrderFromTheErpWhenItIsPlaced(Channel channel)
    {
        await GivenTheCatalogAndSavedAddress(channel, JohnSmithNewYork)
            .When().CheckOut()
            .WithCartContaining(Hp15, 1)
            .WithErpPriceChanged(Hp15, "649.99")
            .WithOrderPlaced()
            .Then().ShouldSucceed()
            .And().OrderFor(Hp15)
            .HasUnitPrice(649.99m);
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-009.6")]
    public async Task ShouldConfirmEveryOrderAndEmptyTheCart(Channel channel)
    {
        await GivenTheCatalogAndSavedAddress(channel, AnaNovakBerlin)
            .When().CheckOut()
            .WithCartContaining(Hp15, 2)
            .WithCartContaining(DellXps, 1)
            .WithOrderPlaced()
            .Then().ShouldSucceed()
            .Checkout()
            .ThanksForTheOrder()
            .ConfirmsOrdersFor(Hp15, DellXps)
            .ConfirmsNothingRejected()
            .LinksEachOrderToItsDetails()
            .OffersViewMyOrdersAndContinueShopping()
            .HasCartLines();
    }
}
