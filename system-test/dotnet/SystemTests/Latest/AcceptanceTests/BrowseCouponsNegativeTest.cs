using SystemTests.Latest.AcceptanceTests.Base;
using Dsl.Core.UseCase;
using Optivem.Testing;
using static SystemTests.Commons.Constants.ErrorMessages;

namespace SystemTests.Latest.AcceptanceTests;

public class BrowseCouponsNegativeTest : BaseAcceptanceTest
{
    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task CustomerShouldNotBeAbleToBrowseCoupons(Channel channel)
    {
        await Scenario(channel)
            .Given().LoggedInAsCustomer()
            .When().BrowseCoupons()
            .Then().ShouldFail()
            .ErrorMessage(PermissionDenied);
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    public async Task ShouldRejectPageSizeAboveTheCap(Channel channel)
    {
        await Scenario(channel)
            .When().BrowseCoupons().WithSize("101")
            .Then().ShouldFail()
            .ErrorMessage("The request contains one or more validation errors")
            .FieldErrorMessage("size", "Page size must be between 1 and 100");
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    public async Task ShouldRejectPageBelowTheFirst(Channel channel)
    {
        await Scenario(channel)
            .When().BrowseCoupons().WithPage("0")
            .Then().ShouldFail()
            .ErrorMessage("The request contains one or more validation errors")
            .FieldErrorMessage("page", "Page must be 1 or greater");
    }
}
