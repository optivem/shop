using static SystemTests.Commons.Constants.ErrorMessages;
using SystemTests.Latest.AcceptanceTests.Base;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

// The cart lives in the browser and only the UI checks it out; there is no checkout API.
//
// Not covered: @UC-009.10 (first order: the address form opens prefilled, "Save as default" is ticked and saves the
// address). It needs a customer with no saved profile, and the two shared test customers' profiles can be saved but
// never cleared, so no scenario can rely on one being empty. The @UC-009.11 scenarios start from a saved address and
// choose "Ship to a different address": the same form and the same rules as on a first order.

/// <summary>Isolated: stages the shared ERP stub's product list and saves the shared test customer's profile.</summary>
[Collection("Isolated")]
[Trait("Category", "isolated")]
public class CheckOutNegativeIsolatedTest : BaseCheckOutTest
{
    private const string PleaseCorrectTheAddress = "Please correct the shipping address.";
    private const string NoOrderWasPlaced = "No order was placed.";

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-009.7")]
    public async Task ShouldAskAnAnonymousVisitorToSignInBeforeCheckout(Channel channel)
    {
        await GivenTheCatalog(channel)
            .And().NotLoggedIn()
            .When().CheckOut()
            .WithCartContaining(Hp15, 1)
            .WithProceededFromCart()
            .Then().ShouldFail()
            .ErrorMessage(AuthenticationRequired);
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-009.7")]
    public async Task ShouldReturnToTheCheckoutWithTheCartUnchangedAfterSignIn(Channel channel)
    {
        await GivenTheCatalog(channel)
            .And().NotLoggedIn()
            .When().CheckOut()
            .WithCartContaining(Hp15, 2)
            .WithCartContaining(LogiMx3S, 1)
            .WithProceededFromCartAndSignedIn()
            .Then().ShouldSucceed()
            .Checkout()
            .IsShown()
            .HasReviewLines(Hp15, LogiMx3S)
            .HasReviewQuantity(Hp15, 2)
            .HasReviewQuantity(LogiMx3S, 1);
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-009.8")]
    public async Task AdminShouldNotBeAbleToCheckOut(Channel channel)
    {
        await GivenTheCatalog(channel)
            .And().LoggedInAsAdmin()
            .When().CheckOut()
            .Then().ShouldFail()
            .ErrorMessage(PermissionDenied);
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-009.9")]
    public async Task ShouldSendTheCustomerToTheCartWhenTheCartIsEmpty(Channel channel)
    {
        await GivenTheCatalog(channel)
            .And().LoggedInAsCustomer()
            .When().CheckOut()
            .Then().ShouldSucceed()
            .Checkout()
            .ShowsTheCart()
            .ShowsMessage("Your cart is empty.");
    }

    public static IEnumerable<object[]> MissingFieldCases()
    {
        yield return new object[] { "first name", "First name is required" };
        yield return new object[] { "last name", "Last name is required" };
        yield return new object[] { "street", "Street is required" };
        yield return new object[] { "postal code", "Postal code is required" };
        yield return new object[] { "city", "City is required" };
        yield return new object[] { "country", "Country is required" };
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [ChannelMemberData(nameof(MissingFieldCases))]
    [Trait("tag", "UC-009.11")]
    public async Task ShouldRejectAnIncompleteShippingAddress(Channel channel, string field, string expectedError)
    {
        var address = JaneBrownLondon.WithEmpty(field);
        await GivenTheCatalogAndSavedAddress(channel, AnaNovakBerlin)
            .When().CheckOut()
            .WithCartContaining(Hp15, 1)
            .WithShippingAddress(address.FirstName, address.LastName, address.Street,
                address.PostalCode, address.City, address.Country)
            .WithOrderPlaced()
            .Then().ShouldSucceed()
            .Checkout()
            .IsShown()
            .ConfirmsNoOrder()
            .ShowsMessage(PleaseCorrectTheAddress)
            .ShowsMessageDetail(expectedError)
            .HasCartLines(Hp15)
            .HasCartLineQuantity(Hp15, 1);
    }

    // The country select offers only US, DE and GB, so FR is sent the way an edited form would send it.
    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-009.11")]
    public async Task ShouldRejectAShippingCountryThatIsNotSupported(Channel channel)
    {
        var address = MarieDupontParis;
        await GivenTheCatalogAndSavedAddress(channel, AnaNovakBerlin)
            .When().CheckOut()
            .WithCartContaining(Hp15, 1)
            .WithShippingAddress(address.FirstName, address.LastName, address.Street,
                address.PostalCode, address.City, address.Country)
            .WithOrderPlaced()
            .Then().ShouldSucceed()
            .Checkout()
            .IsShown()
            .ConfirmsNoOrder()
            .ShowsMessage(PleaseCorrectTheAddress)
            .ShowsMessageDetail("Country must be US, DE or GB");
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-009.12")]
    public async Task ShouldStillPlaceTheOtherLinesWhenTheCouponRunsOutPartWay(Channel channel)
    {
        var then = GivenTheCatalogAndSavedAddress(channel, AnaNovakBerlin)
            .Coupon()
            .WithCouponCode("LIMITED2024")
            .WithUsageLimit(1)
            .When().CheckOut()
            .WithCartContaining(Hp15, 1)
            .WithCartContaining(DellXps, 1)
            .WithCouponCode("LIMITED2024")
            .WithOrderPlaced()
            .Then();

        await then.ShouldSucceed()
            .Checkout()
            .ConfirmsOrdersFor(Hp15)
            .ConfirmsRejected(DellXps, "Coupon code LIMITED2024 has exceeded its usage limit")
            .HasCartLines(DellXps)
            .HasCartLineQuantity(DellXps, 1);

        await then.ShouldSucceed()
            .And().OrderFor(Hp15)
            .HasAppliedCoupon("LIMITED2024");
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-009.12")]
    public async Task ShouldStillPlaceTheOtherLinesWhenAProductIsNoLongerOffered(Channel channel)
    {
        await GivenTheCatalogAndSavedAddress(channel, AnaNovakBerlin)
            .When().CheckOut()
            .WithCartContaining(DellXps, 1)
            .WithCartContaining(LogiMx3S, 2)
            .WithErpNoLongerOffering(DellXps)
            .WithOrderPlaced()
            .Then().ShouldSucceed()
            .Checkout()
            .ConfirmsOrdersFor(LogiMx3S)
            .ConfirmsRejected(DellXps, "Product does not exist for SKU: DELL-XPS")
            .HasCartLines(DellXps)
            .HasCartLineQuantity(DellXps, 1);
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-009.13")]
    public async Task ShouldPlaceNothingAndKeepTheCartWhenEveryLineIsRejected(Channel channel)
    {
        await GivenTheCatalogAndSavedAddress(channel, AnaNovakBerlin)
            .When().CheckOut()
            .WithCartContaining(Hp15, 1)
            .WithCartContaining(DellXps, 1)
            .WithCouponCode("INVALIDCOUPON")
            .WithOrderPlaced()
            .Then().ShouldSucceed()
            .Checkout()
            .IsShown()
            .ConfirmsNoOrder()
            .ShowsMessage(NoOrderWasPlaced)
            .SaysRejected(Hp15, "Coupon code INVALIDCOUPON does not exist")
            .SaysRejected(DellXps, "Coupon code INVALIDCOUPON does not exist")
            .HasCartLines(Hp15, DellXps);
    }
}
