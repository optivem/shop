using SystemTests.Legacy.Mod02.Base;
using Xunit;

namespace SystemTests.Legacy.Mod02.SmokeTests.External;

public class ErpSmokeTest : BaseRawTest
{
    private const string HealthEndpoint = "/health";

    public override async Task InitializeAsync()
    {
        await base.InitializeAsync();
        SetUpExternalHttpClients();
    }

    [Fact]
    public async Task ShouldBeAbleToGoToErp()
    {
        var uri = new Uri(_configuration.ErpBaseUrl + HealthEndpoint);
        var request = new HttpRequestMessage(HttpMethod.Get, uri);

        var response = await _erpHttpClient!.SendAsync(request);

        Assert.Equal(200, (int)response.StatusCode);
    }
}











