using SystemTests.Latest.AcceptanceTests.Base;
using Dsl.Core.UseCase;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

// API only: the UI offers the customers as the order history's customer filter, which
// BrowseOrderHistoryPositiveTest covers.
public class BrowseCustomersPositiveTest : BaseAcceptanceTest
{
    // Shared stack: every customer who has ever ordered is listed, so only A and B's relative order is asserted.
    [Theory]
    [ChannelData(ChannelType.API)]
    [Trait("tag", "UC-011.3")]
    public async Task ShouldListEachCustomerWhoHasPlacedOrdersOnceFromAToZ(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().WithOrderNumber("ORD-A1").PlacedByCustomer("A")
            .And().Order().WithOrderNumber("ORD-B").PlacedByCustomer("B")
            .And().Order().WithOrderNumber("ORD-A2").PlacedByCustomer("A")
            .And().LoggedInAsAdmin()
            .When().BrowseCustomers()
            .Then().ShouldSucceed()
            .CustomerList().ListsCustomersInOrder("A", "B");
    }
}
