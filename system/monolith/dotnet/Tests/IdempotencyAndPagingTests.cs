using System.Net;
using System.Net.Http.Headers;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using Microsoft.Extensions.DependencyInjection;
using MyCompany.MyShop.Monolith.Core.Entities;
using MyCompany.MyShop.Monolith.Data;
using Xunit;

namespace MyCompany.MyShop.Monolith.Tests;

/// <summary>
/// The HTTP half of idempotent order placement and of paging: the <c>Idempotency-Key</c> header reaches
/// the service, a replay answers like the first call (201, Location, body), and paging parameters are
/// validated and echoed in the response envelope. Placing a fresh order needs the external systems, so
/// replays are driven from a stored key; the race itself is an integration test.
/// </summary>
public class IdempotencyAndPagingTests : IClassFixture<ShopWebApplicationFactory>
{
    // ShopWebApplicationFactory mints tokens for this subject.
    private const string Owner = "test-user";
    private const string StoredOrderNumber = "ORD-IDEMPOTENT";
    private const string Key = "idempotency-tests-key";
    private const string RequestBody = "{\"sku\":\"BOOK-123\",\"quantity\":2,\"country\":\"US\"}";

    private readonly ShopWebApplicationFactory _factory;

    public IdempotencyAndPagingTests(ShopWebApplicationFactory factory) => _factory = factory;

    private async Task Seed()
    {
        using var scope = _factory.Services.CreateScope();
        var dbContext = scope.ServiceProvider.GetRequiredService<AppDbContext>();
        // The in-memory database is shared with the other test classes, so seed only what is missing.
        if (!dbContext.IdempotencyKeys.Any(k => k.Owner == Owner && k.Key == Key))
        {
            dbContext.IdempotencyKeys.Add(new IdempotencyKey
            {
                Owner = Owner,
                Key = Key,
                RequestHash = Fingerprint("BOOK-123", "2", "US", ""),
                ResponseStatus = 201,
                ResponseBody = $"{{\"orderNumber\":\"{StoredOrderNumber}\"}}",
                CreatedAt = DateTime.UtcNow
            });
            await dbContext.SaveChangesAsync();
        }
    }

    private async Task<HttpResponseMessage> Send(HttpMethod method, string path, string role, string? body = null, string? key = null)
    {
        await Seed();
        var request = new HttpRequestMessage(method, path);
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", _factory.CreateToken(role));
        if (body != null)
            request.Content = new StringContent(body, Encoding.UTF8, "application/json");
        if (key != null)
            request.Headers.Add("Idempotency-Key", key);
        return await _factory.CreateClient(new() { AllowAutoRedirect = false }).SendAsync(request);
    }

    private static async Task<JsonElement> Json(HttpResponseMessage response) =>
        JsonDocument.Parse(await response.Content.ReadAsStringAsync()).RootElement;

    [Fact]
    public async Task ReplayWithTheSameRequestAnswersLikeTheFirstCall()
    {
        var response = await Send(HttpMethod.Post, "/api/orders", "CUSTOMER", RequestBody, Key);

        Assert.Equal(HttpStatusCode.Created, response.StatusCode);
        Assert.Equal($"/api/orders/{StoredOrderNumber}", response.Headers.Location?.OriginalString);
        Assert.Equal(StoredOrderNumber, (await Json(response)).GetProperty("orderNumber").GetString());
    }

    [Fact]
    public async Task ReplayWithADifferentRequestIsUnprocessable()
    {
        var response = await Send(HttpMethod.Post, "/api/orders", "CUSTOMER",
            "{\"sku\":\"BOOK-123\",\"quantity\":3,\"country\":\"US\"}", Key);

        Assert.Equal(HttpStatusCode.UnprocessableEntity, response.StatusCode);
        Assert.Equal("Idempotency-Key has already been used with a different request",
            (await Json(response)).GetProperty("detail").GetString());
    }

    [Fact]
    public async Task KeyLongerThan255CharactersIsAFieldError()
    {
        var response = await Send(HttpMethod.Post, "/api/orders", "CUSTOMER", RequestBody, new string('k', 256));

        Assert.Equal(HttpStatusCode.UnprocessableEntity, response.StatusCode);
        var error = (await Json(response)).GetProperty("errors")[0];
        Assert.Equal("Idempotency-Key", error.GetProperty("field").GetString());
    }

    [Theory]
    [InlineData("/api/orders")]
    [InlineData("/api/coupons")]
    public async Task ListsDefaultToTheFirstPageOf50(string path)
    {
        var response = await Send(HttpMethod.Get, path, "ADMIN");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var body = await Json(response);
        Assert.Equal(1, body.GetProperty("page").GetInt32());
        Assert.Equal(50, body.GetProperty("size").GetInt32());
        Assert.True(body.TryGetProperty("totalElements", out _));
        Assert.True(body.TryGetProperty("totalPages", out _));
    }

    [Theory]
    [InlineData("/api/orders?size=101", "size", "Page size must be between 1 and 100")]
    [InlineData("/api/orders?page=0", "page", "Page must be 1 or greater")]
    [InlineData("/api/coupons?size=0", "size", "Page size must be between 1 and 100")]
    [InlineData("/api/coupons?page=0", "page", "Page must be 1 or greater")]
    public async Task InvalidPagingIsAFieldError(string path, string field, string message)
    {
        var response = await Send(HttpMethod.Get, path, "ADMIN");

        Assert.Equal(HttpStatusCode.UnprocessableEntity, response.StatusCode);
        var error = (await Json(response)).GetProperty("errors")[0];
        Assert.Equal(field, error.GetProperty("field").GetString());
        Assert.Equal(message, error.GetProperty("message").GetString());
    }

    private static string Fingerprint(string sku, string quantity, string country, string couponCode) =>
        Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes($"{sku}\n{quantity}\n{country}\n{couponCode}")))
            .ToLowerInvariant();
}
