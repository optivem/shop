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
    [Trait("tag", "UC-011.1")]
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
    [Trait("tag", "UC-011.1")]
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

    [Theory]
    [ChannelData(ChannelType.API)]
    [Trait("tag", "UC-011.3")]
    [Trait("tag", "UC-011.4")]
    public async Task AdminShouldSeeOrderOfChosenCustomer(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().WithOrderNumber("ORD-A").PlacedByCustomer("A")
            .And().LoggedInAsAdmin()
            .When().BrowseOrderHistory().WithCustomer("A").WithOrderNumber("ORD-A")
            .Then().ShouldSucceed()
            .OrderHistory().ContainsOrder("ORD-A");
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    [Trait("tag", "UC-011.5")]
    public async Task CustomerFilterShouldBeIgnoredForCustomer(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().WithOrderNumber("ORD-A").PlacedByCustomer("A")
            .And().Order().WithOrderNumber("ORD-B").PlacedByCustomer("B")
            .And().LoggedInAsCustomer("A")
            .When().BrowseOrderHistory().WithCustomer("B").WithOrderNumber("ORD-A")
            .Then().ShouldSucceed()
            .OrderHistory().ContainsOrder("ORD-A");
    }
}
