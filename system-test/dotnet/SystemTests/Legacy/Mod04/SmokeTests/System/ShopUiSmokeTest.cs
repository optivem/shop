using SystemTests.Legacy.Mod04.Base;
using Shouldly;
using Xunit;

namespace SystemTests.Legacy.Mod04.SmokeTests.System;

public class ShopUiSmokeTest : BaseClientTest
{
    public override async Task InitializeAsync()
    {
        await base.InitializeAsync();
        await SetUpShopUiClientAsync();
    }

    [Fact]
    public async Task ShouldBeAbleToGoToShop()
    {
        await _shopUiClient!.OpenHomePageAsync();
        (await _shopUiClient.IsPageLoadedAsync()).ShouldBeTrue();
    }
}










