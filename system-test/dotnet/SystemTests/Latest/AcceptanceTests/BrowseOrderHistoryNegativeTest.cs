using SystemTests.Latest.AcceptanceTests.Base;
using Dsl.Core.UseCase;
using Optivem.Testing;
using static Dsl.Core.Scenario.ScenarioDefaults;

namespace SystemTests.Latest.AcceptanceTests;

public class BrowseOrderHistoryNegativeTest : BaseAcceptanceTest
{
    [RequiresKeycloakTheory]
    [ChannelData(ChannelType.API)]
    public async Task CustomerShouldNotSeeAnotherCustomersOrderInHistory(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().PlacedByCustomer("B")
            .And().LoggedInAsCustomer("A")
            .When().BrowseOrderHistory()
            .Then().ShouldSucceed()
            .OrderHistory().DoesNotContainOrder(DefaultOrderNumber);
    }
}
