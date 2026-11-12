using SystemTests.Latest.AcceptanceTests.Base;
using Dsl.Core.UseCase;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

public class BrowseCouponsPositiveTest : BaseAcceptanceTest
{
    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task ShouldBeAbleToBrowseCoupons(Channel channel)
    {
        await Scenario(channel)
            .When().BrowseCoupons()
            .Then().ShouldSucceed();
    }
}
