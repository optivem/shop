using SystemTests.Legacy.Mod02.Base;
using Xunit;

namespace SystemTests.Legacy.Mod02.SmokeTests.System;

public class ShopApiSmokeTest : BaseRawTest
{
    private const string HealthEndpoint = "/health";

    public override async Task InitializeAsync()
    {
        await base.InitializeAsync();
        SetUpShopHttpClient();
    }

    [Fact]
    public async Task ShouldBeAbleToGoToShop()
    {
        var uri = new Uri(_configuration.ShopApiBaseUrl + HealthEndpoint);
        var request = new HttpRequestMessage(HttpMethod.Get, uri);

        var response = await _shopApiHttpClient!.SendAsync(request);

        Assert.Equal(200, (int)response.StatusCode);
    }
}











