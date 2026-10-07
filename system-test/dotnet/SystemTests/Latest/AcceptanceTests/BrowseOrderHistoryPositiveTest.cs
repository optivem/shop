using SystemTests.Latest.AcceptanceTests.Base;
using Dsl.Core.UseCase;
using Optivem.Testing;
using static Dsl.Core.Scenario.ScenarioDefaults;

namespace SystemTests.Latest.AcceptanceTests;

public class BrowseOrderHistoryPositiveTest : BaseAcceptanceTest
{
    [Theory]
    [ChannelData(ChannelType.API)]
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
    [ChannelData(ChannelType.API)]
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
    [ChannelData(ChannelType.API)]
    public async Task AdminShouldSeeAnotherCustomersOrderInHistory(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().PlacedByCustomer("B")
            .And().LoggedInAsAdmin()
            .When().BrowseOrderHistory()
            .Then().ShouldSucceed()
            .OrderHistory().ContainsOrder(DefaultOrderNumber);
    }
}
