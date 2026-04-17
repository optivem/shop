using SystemTests.Legacy.Mod08.Base;
using Optivem.Testing;
using Xunit;

namespace SystemTests.Legacy.Mod08.SmokeTests.System;

public class ShopSmokeTest : BaseScenarioDslTest
{
    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task ShouldBeAbleToGoToShop(Channel channel)
    {
        await Scenario(channel)
            .Assume().Shop().ShouldBeRunning();
    }
}
