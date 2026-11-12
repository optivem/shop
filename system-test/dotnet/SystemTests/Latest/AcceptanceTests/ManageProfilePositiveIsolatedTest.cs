using SystemTests.Latest.AcceptanceTests.Base;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

/// <summary>Isolated: the two test customers' profiles are shared state, so profile scenarios must not run in parallel.</summary>
[Collection("Isolated")]
[Trait("Category", "isolated")]
public class ManageProfilePositiveIsolatedTest : BaseAcceptanceTest
{
    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-010.1")]
    public async Task ShouldShowSavedProfile(Channel channel)
    {
        await Scenario(channel)
            .Given().Profile()
            .WithFirstName("Ana").WithLastName("Novak")
            .WithStreet("Main St 1").WithPostalCode("10115").WithCity("Berlin").WithCountry("DE")
            .SavedByCustomer()
            .And().LoggedInAsCustomer()
            .When().ViewProfile()
            .Then().ShouldSucceed()
            .Profile()
            .HasFirstName("Ana")
            .HasLastName("Novak")
            .HasStreet("Main St 1")
            .HasPostalCode("10115")
            .HasCity("Berlin")
            .HasCountry("DE")
            .HasEmail("customer1@example.com");
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-010.1")]
    public async Task ShouldNotLetTheCustomerChangeTheEmail(Channel channel)
    {
        await Scenario(channel)
            .Given().LoggedInAsCustomer()
            .When().ViewProfile()
            .Then().ShouldSucceed()
            .Profile()
            .HasEmail("customer1@example.com")
            .HasEmailThatCannotBeChanged();
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-010.1")]
    public async Task ShouldShowChangedProfileAfterSaving(Channel channel)
    {
        var then = Scenario(channel)
            .Given().Profile().WithCountry("DE").SavedByCustomer()
            .And().LoggedInAsCustomer()
            .When().UpdateProfile().WithStreet("1 High St").WithPostalCode("SW1A 1AA").WithCity("London").WithCountry("GB")
            .Then();

        await then.ShouldSucceed()
            .Profile()
            .HasStreet("1 High St")
            .HasPostalCode("SW1A 1AA")
            .HasCity("London")
            .HasCountry("GB");

        await then.ShouldSucceed()
            .And().Profile()
            .HasStreet("1 High St")
            .HasPostalCode("SW1A 1AA")
            .HasCity("London")
            .HasCountry("GB");
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-010.1")]
    public async Task ShouldTellTheCustomerTheProfileWasSaved(Channel channel)
    {
        await Scenario(channel)
            .Given().LoggedInAsCustomer()
            .When().UpdateProfile()
            .Then().ShouldSucceed()
            .Profile()
            .HasMessage("Your profile has been saved.");
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-010.3")]
    public async Task ShouldOfferOnlyTheSupportedShippingCountries(Channel channel)
    {
        await Scenario(channel)
            .Given().LoggedInAsCustomer()
            .When().ViewProfile()
            .Then().ShouldSucceed()
            .Profile()
            .HasCountryChoices("US", "DE", "GB");
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-010.4")]
    public async Task ShouldTellTheCustomerThatTaxFollowsTheShippingCountry(Channel channel)
    {
        await Scenario(channel)
            .Given().LoggedInAsCustomer()
            .When().ViewProfile()
            .Then().ShouldSucceed()
            .Profile()
            .HasTaxNotice("Sales tax is charged at the rate of your shipping address's country.");
    }

    // The saved address as checkout's default (the other @UC-010.5 scenario) is CheckOutPositiveIsolatedTest's
    // ShouldPreselectTheSavedShippingAddress.
    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-010.5")]
    public async Task ShouldPrefillTheQuickOrderCountryFromTheProfile(Channel channel)
    {
        await Scenario(channel)
            .Given().Profile().WithCountry("GB").SavedByCustomer()
            .And().LoggedInAsCustomer()
            .When().PlaceOrder().WithPrefilledCountry()
            .Then().ShouldSucceed()
            .And().Order()
            .HasCountryCode("GB");
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-010.5")]
    public async Task ShouldKeepEachCustomersProfilePrivate(Channel channel)
    {
        await Scenario(channel)
            .Given().Profile().WithStreet("Private Lane 7").SavedByCustomer("A")
            .And().LoggedInAsCustomer("B")
            .When().ViewProfile()
            .Then().ShouldSucceed()
            .Profile()
            .DoesNotHaveStreet("Private Lane 7");
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-010.6")]
    public async Task ShouldAskAnAnonymousVisitorToSignInAndReturnToTheProfile(Channel channel)
    {
        await Scenario(channel)
            .Given().NotLoggedIn()
            .When().ViewProfile().WithSignInWhenAsked()
            .Then().ShouldSucceed()
            .Profile()
            .HasEmail("customer1@example.com");
    }

    // The new customer's email is unique to the run, so the email shown is checked by ShouldShowSavedProfile.
    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-010.7")]
    public async Task ShouldShowAnEmptyProfileWithNamesFromTheSignInAccount(Channel channel)
    {
        await Scenario(channel)
            .Given().NewCustomer()
            .WithFirstName("Ana")
            .WithLastName("Novak")
            .And().LoggedInAsCustomer()
            .When().ViewProfile()
            .Then().ShouldSucceed()
            .Profile()
            .HasFirstName("Ana")
            .HasLastName("Novak")
            .HasNoSavedAddress();
    }
}
