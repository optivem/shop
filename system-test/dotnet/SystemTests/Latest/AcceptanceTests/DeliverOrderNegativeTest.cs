using SystemTests.Latest.AcceptanceTests.Base;
using Dsl.Core.UseCase;
using Optivem.Testing;
using static SystemTests.Commons.Constants.ErrorMessages;

namespace SystemTests.Latest.AcceptanceTests;

public class DeliverOrderNegativeTest : BaseAcceptanceTest
{
    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task CustomerShouldNotBeAbleToDeliverOrder(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().PlacedByCustomer()
            .And().LoggedInAsCustomer()
            .When().DeliverOrder()
            .Then().ShouldFail()
            .ErrorMessage(PermissionDenied);
    }
}
