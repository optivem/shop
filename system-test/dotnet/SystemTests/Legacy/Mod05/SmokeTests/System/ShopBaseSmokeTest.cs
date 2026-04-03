using Common;
using SystemTests.Legacy.Mod05.Base;
using Shouldly;
using Xunit;

namespace SystemTests.Legacy.Mod05.SmokeTests.System;

public abstract class ShopBaseSmokeTest : BaseDriverTest
{
    protected abstract Task SetUpShopDriverAsync();

    public override async Task InitializeAsync()
    {
        await base.InitializeAsync();
        await SetUpShopDriverAsync();
    }

    [Fact]
    public async Task ShouldBeAbleToGoToShop()
    {
        var result = await _shopDriver!.GoToShopAsync();
        result.ShouldBeSuccess();
    }
}










