using System.Net;
using System.Net.Http.Headers;
using Xunit;

namespace MyCompany.MyShop.Monolith.Tests;

public class SecurityTests : IClassFixture<ShopWebApplicationFactory>
{
    private readonly ShopWebApplicationFactory _factory;

    public SecurityTests(ShopWebApplicationFactory factory) => _factory = factory;

    private HttpClient Client(string? token = null)
    {
        var client = _factory.CreateClient(new() { AllowAutoRedirect = false });
        if (token != null)
            client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);
        return client;
    }

    [Fact]
    public async Task Health_IsPublic()
    {
        var response = await Client().GetAsync("/health");
        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
    }

    [Theory]
    [InlineData("/api/orders")]
    [InlineData("/api/coupons")]
    public async Task Api_WithoutToken_Returns401(string path)
    {
        var response = await Client().GetAsync(path);
        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
        Assert.Equal("application/problem+json", response.Content.Headers.ContentType?.MediaType);
    }

    [Fact]
    public async Task Page_WithoutSession_RedirectsToKeycloak()
    {
        var response = await Client().GetAsync("/");
        Assert.Equal(HttpStatusCode.Redirect, response.StatusCode);
        var location = response.Headers.Location!.ToString();
        Assert.StartsWith(ShopWebApplicationFactory.Issuer + "/protocol/openid-connect/auth", location);
        Assert.Contains("code_challenge_method=S256", location);
    }

    [Fact]
    public async Task Orders_WithCustomerToken_Returns200()
    {
        var response = await Client(_factory.CreateToken("CUSTOMER")).GetAsync("/api/orders");
        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
    }

    [Fact]
    public async Task PlaceOrder_WithAdminToken_Returns403()
    {
        var response = await Client(_factory.CreateToken("ADMIN"))
            .PostAsync("/api/orders", new StringContent("{}", System.Text.Encoding.UTF8, "application/json"));
        Assert.Equal(HttpStatusCode.Forbidden, response.StatusCode);
    }

    [Fact]
    public async Task Coupons_WithCustomerToken_Returns403()
    {
        var response = await Client(_factory.CreateToken("CUSTOMER")).GetAsync("/api/coupons");
        Assert.Equal(HttpStatusCode.Forbidden, response.StatusCode);
    }

    [Fact]
    public async Task Coupons_WithAdminToken_Returns200()
    {
        var response = await Client(_factory.CreateToken("ADMIN")).GetAsync("/api/coupons");
        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
    }

    [Fact]
    public async Task Deliver_WithCustomerToken_Returns403()
    {
        var response = await Client(_factory.CreateToken("CUSTOMER")).PostAsync("/api/orders/ORD-1/deliver", null);
        Assert.Equal(HttpStatusCode.Forbidden, response.StatusCode);
    }

    [Fact]
    public async Task WrongAudience_Returns401()
    {
        var token = _factory.CreateToken("ADMIN", audience: "someone-else");
        var response = await Client(token).GetAsync("/api/orders");
        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task ExpiredToken_Returns401()
    {
        var token = _factory.CreateToken("ADMIN", lifetime: TimeSpan.FromMinutes(-5));
        var response = await Client(token).GetAsync("/api/orders");
        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }
}
