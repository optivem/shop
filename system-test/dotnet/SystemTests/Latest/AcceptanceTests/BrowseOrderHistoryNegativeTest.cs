using SystemTests.Latest.AcceptanceTests.Base;
using Dsl.Core.UseCase;
using Optivem.Testing;
using static Dsl.Core.Scenario.ScenarioDefaults;

namespace SystemTests.Latest.AcceptanceTests;

public class BrowseOrderHistoryNegativeTest : BaseAcceptanceTest
{
    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-002.6")]
    public async Task CustomerShouldNotSeeAnotherCustomersOrderInHistory(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().PlacedByCustomer("B")
            .And().LoggedInAsCustomer("A")
            .When().BrowseOrderHistory()
            .Then().ShouldSucceed()
            .OrderHistory().DoesNotContainOrder(DefaultOrderNumber);
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    [Trait("tag", "UC-002.8")]
    public async Task ShouldRejectPageSizeAboveTheCap(Channel channel)
    {
        await Scenario(channel)
            .When().BrowseOrderHistory().WithSize("101")
            .Then().ShouldFail()
            .ErrorMessage("The request contains one or more validation errors")
            .FieldErrorMessage("size", "Page size must be between 1 and 100");
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    [Trait("tag", "UC-002.8")]
    public async Task ShouldRejectPageBelowTheFirst(Channel channel)
    {
        await Scenario(channel)
            .When().BrowseOrderHistory().WithPage("0")
            .Then().ShouldFail()
            .ErrorMessage("The request contains one or more validation errors")
            .FieldErrorMessage("page", "Page must be 1 or greater");
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    [Trait("tag", "UC-002.8")]
    public async Task ShouldRejectPageSizeBelowOne(Channel channel)
    {
        await Scenario(channel)
            .When().BrowseOrderHistory().WithSize("0")
            .Then().ShouldFail()
            .ErrorMessage("The request contains one or more validation errors")
            .FieldErrorMessage("size", "Page size must be between 1 and 100");
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    [Trait("tag", "UC-002.9")]
    public async Task ShouldShowNoOrdersWhenNoOrderMatchesTheFilter(Channel channel)
    {
        await Scenario(channel)
            .Given().LoggedInAsCustomer()
            .When().BrowseOrderHistory().WithOrderNumber("NON-EXISTENT-ORDER-99999")
            .Then().ShouldSucceed()
            .OrderHistory().IsEmpty();
    }
}
