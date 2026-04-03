using Common;
using SystemTests.Legacy.Mod04.Base;
using Xunit;

namespace SystemTests.Legacy.Mod04.SmokeTests.System;

public class ShopApiSmokeTest : BaseClientTest
{
    public override async Task InitializeAsync()
    {
        await base.InitializeAsync();
        SetUpShopApiClient();
    }

    [Fact]
    public async Task ShouldBeAbleToGoToShop()
    {
        var result = await _shopApiClient!.Health().CheckHealthAsync();
        result.ShouldBeSuccess();
    }
}










