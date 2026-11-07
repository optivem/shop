using Dsl.Core.UseCase;
using SystemTests.Latest.E2eTests.Base;
using Optivem.Testing;
using Xunit;

namespace SystemTests.Latest.E2eTests;

public class PlaceOrderPositiveTest : BaseE2eTest
{
    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-001.1")]
    public async Task ShouldPlaceOrder(Channel channel)
    {
        await Scenario(channel)
            .When().PlaceOrder()
            .Then().ShouldSucceed();
    }
}











