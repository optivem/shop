using static SystemTests.Commons.Constants.ErrorMessages;
using SystemTests.Latest.AcceptanceTests.Base;
using Dsl.Port.When.Steps;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

/// <summary>Isolated: the two test customers' profiles are shared state, so profile scenarios must not run in parallel.</summary>
[Collection("Isolated")]
[Trait("Category", "isolated")]
public class ManageProfileNegativeIsolatedTest : BaseAcceptanceTest
{
    private const string ValidationErrors = "The request contains one or more validation errors";

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [ChannelInlineData("firstName", "First name is required")]
    [ChannelInlineData("lastName", "Last name is required")]
    [ChannelInlineData("street", "Street is required")]
    [ChannelInlineData("postalCode", "Postal code is required")]
    [ChannelInlineData("city", "City is required")]
    [ChannelInlineData("country", "Country is required")]
    [Trait("tag", "UC-010.2")]
    public async Task ShouldRequireEveryNameAndAddressField(Channel channel, string field, string message)
    {
        await WithField(Scenario(channel).Given().LoggedInAsCustomer().When().UpdateProfile(), field, "")
            .Then().ShouldFail()
            .ErrorMessage(ValidationErrors)
            .FieldErrorMessage(field, message);
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-010.2")]
    public async Task ShouldTreatProfileFieldOfOnlySpacesAsEmpty(Channel channel)
    {
        await Scenario(channel)
            .Given().LoggedInAsCustomer()
            .When().UpdateProfile().WithCity("   ")
            .Then().ShouldFail()
            .ErrorMessage(ValidationErrors)
            .FieldErrorMessage("city", "City is required");
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-010.3")]
    public async Task ShouldRejectUnsupportedProfileCountry(Channel channel)
    {
        await Scenario(channel)
            .Given().LoggedInAsCustomer()
            .When().UpdateProfile().WithCountry("FR")
            .Then().ShouldFail()
            .ErrorMessage(ValidationErrors)
            .FieldErrorMessage("country", "Country must be US, DE or GB");
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-010.8")]
    public async Task ShouldKeepSavedProfileWhenInvalidChangeIsRejected(Channel channel)
    {
        await Scenario(channel)
            .Given().Profile().WithCountry("DE").SavedByCustomer()
            .And().LoggedInAsCustomer()
            .When().UpdateProfile().WithStreet("")
            .Then().ShouldFail()
            .ErrorMessage(ValidationErrors)
            .FieldErrorMessage("street", "Street is required")
            .And().Profile()
            .HasStreet("Main St 1")
            .HasPostalCode("10115")
            .HasCity("Berlin")
            .HasCountry("DE");
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task AnonymousVisitorShouldNotBeAbleToViewProfile(Channel channel)
    {
        await Scenario(channel)
            .Given().NotLoggedIn()
            .When().ViewProfile()
            .Then().ShouldFail()
            .ErrorMessage(AuthenticationRequired);
    }

    private static IWhenUpdateProfile WithField(IWhenUpdateProfile updateProfile, string field, string value) => field switch
    {
        "firstName" => updateProfile.WithFirstName(value),
        "lastName" => updateProfile.WithLastName(value),
        "street" => updateProfile.WithStreet(value),
        "postalCode" => updateProfile.WithPostalCode(value),
        "city" => updateProfile.WithCity(value),
        "country" => updateProfile.WithCountry(value),
        _ => throw new ArgumentOutOfRangeException(nameof(field), field, "Not a profile field")
    };
}
