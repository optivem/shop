using Xunit;

namespace MyCompany.MyShop.Monolith.Tests;

public class MonolithApplicationTests : IClassFixture<ShopWebApplicationFactory>
{
    private readonly ShopWebApplicationFactory _factory;

    public MonolithApplicationTests(ShopWebApplicationFactory factory)
    {
        _factory = factory;
    }

    [Fact]
    public async Task HealthEndpoint_ReturnsUp()
    {
        var client = _factory.CreateClient();
        var response = await client.GetAsync("/health");
        response.EnsureSuccessStatusCode();
    }
}
