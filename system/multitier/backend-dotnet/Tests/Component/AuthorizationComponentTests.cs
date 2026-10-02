using System.Net;
using System.Net.Http.Headers;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using MyCompany.MyShop.Backend.Data;
using MyCompany.MyShop.Backend.Tests.Support;
using Xunit;

namespace MyCompany.MyShop.Backend.Tests.Component;

/// <summary>
/// Drives the real authentication and authorization pipeline over HTTP with locally signed tokens:
/// which endpoints are public, which need a token, which need the ADMIN role, and how rejected
/// tokens are reported.
/// </summary>
public class AuthorizationComponentTests : IClassFixture<WebApplicationFactory<Program>>
{
    private readonly WebApplicationFactory<Program> _factory;

    public AuthorizationComponentTests(WebApplicationFactory<Program> factory)
    {
        _factory = factory.WithWebHostBuilder(builder =>
        {
            builder.UseTestAuth();
            builder.ConfigureServices(services =>
            {
                var descriptor = services.SingleOrDefault(
                    d => d.ServiceType == typeof(DbContextOptions<AppDbContext>));
                if (descriptor != null)
                    services.Remove(descriptor);

                services.AddDbContext<AppDbContext>(options =>
                    options.UseInMemoryDatabase("AuthorizationTestDb"));
            });
        });
    }

    private async Task<HttpResponseMessage> Call(HttpMethod method, string path, string? token = null)
    {
        var client = _factory.CreateClient();
        var request = new HttpRequestMessage(method, path);
        if (token != null)
            request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);
        if (method == HttpMethod.Post)
            request.Content = new StringContent("{}", System.Text.Encoding.UTF8, "application/json");
        return await client.SendAsync(request);
    }

    [Fact]
    public async Task HealthIsPublic()
    {
        var response = await Call(HttpMethod.Get, "/health");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
    }

    [Fact]
    public async Task OrdersWithoutTokenReturnsUnauthorizedProblemDetail()
    {
        var response = await Call(HttpMethod.Get, "/api/orders");

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
        Assert.Equal("application/problem+json", response.Content.Headers.ContentType?.MediaType);
        Assert.Contains("Bearer", response.Headers.WwwAuthenticate.ToString());
        var body = await response.Content.ReadAsStringAsync();
        Assert.Contains("\"status\":401", body);
        Assert.Contains("\"title\":\"Unauthorized\"", body);
    }

    [Fact]
    public async Task OrdersWithGarbageTokenReturnsUnauthorized()
    {
        var response = await Call(HttpMethod.Get, "/api/orders", "garbage");

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task TokenWithWrongAudienceIsRejected()
    {
        var token = TestAuth.CreateToken(["ADMIN"], audience: "some-other-api");

        var response = await Call(HttpMethod.Get, "/api/orders", token);

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task TokenWithWrongIssuerIsRejected()
    {
        var token = TestAuth.CreateToken(["ADMIN"], issuer: "http://evil.example/realms/shop");

        var response = await Call(HttpMethod.Get, "/api/orders", token);

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task ExpiredTokenIsRejected()
    {
        var token = TestAuth.CreateToken(["ADMIN"], expires: DateTime.UtcNow.AddMinutes(-5));

        var response = await Call(HttpMethod.Get, "/api/orders", token);

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task TokenSignedWithUntrustedKeyIsRejected()
    {
        var token = TestAuth.CreateTokenSignedByStranger(["ADMIN"]);

        var response = await Call(HttpMethod.Get, "/api/orders", token);

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task UnsignedTokenIsRejected()
    {
        var token = TestAuth.CreateUnsignedToken(["ADMIN"]);

        var response = await Call(HttpMethod.Get, "/api/orders", token);

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task CustomerCanBrowseOrders()
    {
        var response = await Call(HttpMethod.Get, "/api/orders", TestAuth.CustomerToken);

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
    }

    [Fact]
    public async Task CustomerCannotDeliverOrder()
    {
        var response = await Call(HttpMethod.Post, "/api/orders/ORD-1/deliver", TestAuth.CustomerToken);

        Assert.Equal(HttpStatusCode.Forbidden, response.StatusCode);
        var body = await response.Content.ReadAsStringAsync();
        Assert.Contains("\"status\":403", body);
        Assert.Contains("\"title\":\"Forbidden\"", body);
    }

    [Fact]
    public async Task CustomerCannotBrowseCoupons()
    {
        var response = await Call(HttpMethod.Get, "/api/coupons", TestAuth.CustomerToken);

        Assert.Equal(HttpStatusCode.Forbidden, response.StatusCode);
    }

    [Fact]
    public async Task AdminCanBrowseCoupons()
    {
        var response = await Call(HttpMethod.Get, "/api/coupons", TestAuth.AdminToken);

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
    }

    [Fact]
    public async Task CustomerCannotPublishCoupon()
    {
        var response = await Call(HttpMethod.Post, "/api/coupons", TestAuth.CustomerToken);

        Assert.Equal(HttpStatusCode.Forbidden, response.StatusCode);
    }

    [Fact]
    public async Task CustomerCannotUseAdminEndpoints()
    {
        var response = await Call(HttpMethod.Post, "/api/admin/recall/BOOK-123", TestAuth.CustomerToken);

        Assert.Equal(HttpStatusCode.Forbidden, response.StatusCode);
    }

    [Fact]
    public async Task AdminPassesAuthorizationOnAdminEndpoint()
    {
        // The order does not exist, so the request is authorized and then rejected by the service.
        var response = await Call(HttpMethod.Post, "/api/orders/ORD-MISSING/deliver", TestAuth.AdminToken);

        Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
    }

    [Fact]
    public async Task CorsPreflightPassesWithoutToken()
    {

        var request = new HttpRequestMessage(HttpMethod.Options, "/api/orders");
        request.Headers.Add("Origin", "http://localhost:3000");
        request.Headers.Add("Access-Control-Request-Method", "GET");
        request.Headers.Add("Access-Control-Request-Headers", "authorization");

        var response = await _factory.CreateClient().SendAsync(request);

        Assert.True(response.IsSuccessStatusCode, $"Preflight returned {(int)response.StatusCode}");
    }
}
