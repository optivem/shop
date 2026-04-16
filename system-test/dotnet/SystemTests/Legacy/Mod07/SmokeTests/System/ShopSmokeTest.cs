using Common;
using SystemTests.Legacy.Mod07.Base;
using Dsl.Core.Shop;
using Optivem.Testing;
using Shouldly;
using Xunit;

namespace SystemTests.Legacy.Mod07.SmokeTests.System;

public class ShopSmokeTest : BaseUseCaseDslTest
{
    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task ShouldBeAbleToGoToShop(Channel channel)
    {
        (await (await _app.Shop(channel)).GoToShop()
            .Execute())
            .ShouldSucceed();
    }
}











