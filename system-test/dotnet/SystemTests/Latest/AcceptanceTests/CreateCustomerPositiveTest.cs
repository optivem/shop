using SystemTests.Latest.AcceptanceTests.Base;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

// UC-013: the visitor creates the account through the shop's "Sign up" link and Keycloak's registration form, so
// these scenarios run on the UI channel only. Every other scenario keeps creating customers through the admin API.
public class CreateCustomerPositiveTest : BaseAcceptanceTest
{
    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-013.1")]
    public async Task ShouldTakeTheNameAndShippingCountryFromTheSignUp(Channel channel)
    {
        await Scenario(channel)
            .Given().NewCustomer()
            .WithFirstName("Ana")
            .WithLastName("Novak")
            .WithCountry("GB")
            .SignedUpThroughTheShop()
            .And().LoggedInAsCustomer()
            .When().ViewProfile()
            .Then().ShouldSucceed()
            .Profile()
            .HasFirstName("Ana")
            .HasLastName("Novak")
            .HasCountry("GB")
            .HasNoStreetAddress();
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-013.2")]
    public async Task ShouldLetTheNewCustomerPlaceAnOrderStraightAway(Channel channel)
    {
        await Scenario(channel)
            .Given().NewCustomer()
            .WithCountry("GB")
            .SignedUpThroughTheShop()
            .And().LoggedInAsCustomer()
            .When().PlaceOrder().WithPrefilledCountry()
            .Then().ShouldSucceed()
            .And().Order()
            .HasCountryCode("GB");
    }
}
