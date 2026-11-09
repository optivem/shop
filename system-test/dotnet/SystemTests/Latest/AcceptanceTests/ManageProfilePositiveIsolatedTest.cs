using SystemTests.Latest.AcceptanceTests.Base;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

/// <summary>Isolated: the two test customers' profiles are shared state, so profile scenarios must not run in parallel.</summary>
[Collection("Isolated")]
[Trait("Category", "isolated")]
public class ManageProfilePositiveIsolatedTest : BaseAcceptanceTest
{
    [Theory]
    [ChannelData(ChannelType.API)]
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
    [ChannelData(ChannelType.API)]
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
    [ChannelData(ChannelType.API)]
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
}
