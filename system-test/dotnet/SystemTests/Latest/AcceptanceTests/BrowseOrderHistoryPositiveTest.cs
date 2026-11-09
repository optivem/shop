using SystemTests.Latest.AcceptanceTests.Base;
using Dsl.Core.UseCase;
using Optivem.Testing;
using static Dsl.Core.Scenario.ScenarioDefaults;

namespace SystemTests.Latest.AcceptanceTests;

public class BrowseOrderHistoryPositiveTest : BaseAcceptanceTest
{
    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-002.1")]
    [Trait("tag", "UC-002.2")]
    public async Task CustomerShouldSeeOwnOrderInHistory(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().PlacedByCustomer()
            .And().LoggedInAsCustomer()
            .When().BrowseOrderHistory()
            .Then().ShouldSucceed()
            .OrderHistory().ContainsOrder(DefaultOrderNumber);
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task AdminShouldSeeCustomerOrderInHistory(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().PlacedByCustomer()
            .And().LoggedInAsAdmin()
            .When().BrowseOrderHistory()
            .Then().ShouldSucceed()
            .OrderHistory().ContainsOrder(DefaultOrderNumber);
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task AdminShouldSeeAnotherCustomersOrderInHistory(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().PlacedByCustomer("B")
            .And().LoggedInAsAdmin()
            .When().BrowseOrderHistory()
            .Then().ShouldSucceed()
            .OrderHistory().ContainsOrder(DefaultOrderNumber);
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    [Trait("tag", "UC-002.3")]
    public async Task FirstPageShouldReportTheTotalNumberOfMatchingOrders(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().PlacedByCustomer()
            .And().LoggedInAsCustomer()
            .When().BrowseOrderHistory().WithPage("1").WithSize("1")
            .Then().ShouldSucceed()
            .OrderHistory().ContainsOrder(DefaultOrderNumber).HasTotalElements(1);
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    [Trait("tag", "UC-002.3")]
    public async Task PagePastTheLastShouldBeEmpty(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().PlacedByCustomer()
            .And().LoggedInAsCustomer()
            .When().BrowseOrderHistory().WithPage("2")
            .Then().ShouldSucceed()
            .OrderHistory().IsEmpty();
    }
}
