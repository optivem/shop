using System.Net;
using System.Net.Http.Headers;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using MyCompany.MyShop.Backend.Core.Entities;
using MyCompany.MyShop.Backend.Data;
using MyCompany.MyShop.Backend.Tests.Support;
using Xunit;

namespace MyCompany.MyShop.Backend.Tests.Component;

/// <summary>
/// The HTTP half of idempotent order placement and of paging: the <c>Idempotency-Key</c> header reaches
/// the service, a replay answers like the first call (201, Location, body), and paging parameters are
/// validated and echoed in the response envelope. Placing a fresh order needs the external systems, so
/// replays are driven from a stored key; the race itself is an integration test.
/// </summary>
public class IdempotencyAndPagingComponentTests : IClassFixture<WebApplicationFactory<Program>>
{
    private const string Customer = "idempotency-customer";
    private const string StoredOrderNumber = "ORD-IDEMPOTENT";
    private const string Key = "key-1";
    private const string RequestBody = "{\"sku\":\"BOOK-123\",\"quantity\":2,\"country\":\"US\"}";

    private readonly WebApplicationFactory<Program> _factory;

    public IdempotencyAndPagingComponentTests(WebApplicationFactory<Program> factory)
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
                    options.UseInMemoryDatabase("IdempotencyAndPagingTestDb"));
            });
        });
    }

    private async Task Seed()
    {
        using var scope = _factory.Services.CreateScope();
        var dbContext = scope.ServiceProvider.GetRequiredService<AppDbContext>();
        dbContext.IdempotencyKeys.RemoveRange(dbContext.IdempotencyKeys);
        dbContext.Orders.RemoveRange(dbContext.Orders);
        dbContext.Coupons.RemoveRange(dbContext.Coupons);
        dbContext.IdempotencyKeys.Add(new IdempotencyKey
        {
            Owner = Customer,
            Key = Key,
            RequestHash = Fingerprint("BOOK-123", "2", "US", ""),
            ResponseStatus = 201,
            ResponseBody = $"{{\"orderNumber\":\"{StoredOrderNumber}\"}}",
            CreatedAt = DateTime.UtcNow
        });
        for (var i = 1; i <= 3; i++)
        {
            dbContext.Orders.Add(NewOrder($"ORD-PAGE-{i}", new DateTime(2026, 3, 10, 12, i, 0, DateTimeKind.Utc)));
            dbContext.Coupons.Add(new Coupon { Code = $"PAGE-{i}", DiscountRate = 0.1m, UsedCount = 0 });
        }
        await dbContext.SaveChangesAsync();
    }

    private async Task<HttpResponseMessage> PlaceOrder(string body, string? key)
    {
        await Seed();
        var request = new HttpRequestMessage(HttpMethod.Post, "/api/orders")
        {
            Content = new StringContent(body, Encoding.UTF8, "application/json")
        };
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", TestAuth.CustomerTokenFor(Customer));
        if (key != null)
            request.Headers.Add("Idempotency-Key", key);
        return await _factory.CreateClient().SendAsync(request);
    }

    private async Task<HttpResponseMessage> Get(string path)
    {
        await Seed();
        var request = new HttpRequestMessage(HttpMethod.Get, path);
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", TestAuth.AdminToken);
        return await _factory.CreateClient().SendAsync(request);
    }

    private static async Task<JsonElement> Json(HttpResponseMessage response) =>
        JsonDocument.Parse(await response.Content.ReadAsStringAsync()).RootElement;

    [Fact]
    public async Task ReplayWithTheSameRequestAnswersLikeTheFirstCall()
    {
        var response = await PlaceOrder(RequestBody, Key);

        Assert.Equal(HttpStatusCode.Created, response.StatusCode);
        Assert.Equal($"/api/orders/{StoredOrderNumber}", response.Headers.Location?.OriginalString);
        Assert.Equal(StoredOrderNumber, (await Json(response)).GetProperty("orderNumber").GetString());
    }

    [Fact]
    public async Task ReplayWithADifferentRequestIsUnprocessable()
    {
        var response = await PlaceOrder("{\"sku\":\"BOOK-123\",\"quantity\":3,\"country\":\"US\"}", Key);

        Assert.Equal(HttpStatusCode.UnprocessableEntity, response.StatusCode);
        Assert.Equal("Idempotency-Key has already been used with a different request",
            (await Json(response)).GetProperty("detail").GetString());
    }

    [Fact]
    public async Task KeyLongerThan255CharactersIsAFieldError()
    {
        var response = await PlaceOrder(RequestBody, new string('k', 256));

        Assert.Equal(HttpStatusCode.UnprocessableEntity, response.StatusCode);
        var error = (await Json(response)).GetProperty("errors")[0];
        Assert.Equal("Idempotency-Key", error.GetProperty("field").GetString());
        Assert.Equal("Idempotency-Key must be at most 255 characters", error.GetProperty("message").GetString());
    }

    [Theory]
    [InlineData("/api/orders")]
    [InlineData("/api/coupons")]
    public async Task ListsDefaultToTheFirstPageOf50(string path)
    {
        var body = await Json(await Get(path));

        Assert.Equal(1, body.GetProperty("page").GetInt32());
        Assert.Equal(50, body.GetProperty("size").GetInt32());
        Assert.Equal(3, body.GetProperty("totalElements").GetInt64());
        Assert.Equal(1, body.GetProperty("totalPages").GetInt32());
    }

    [Fact]
    public async Task OrderHistoryPagesNewestFirst()
    {
        var body = await Json(await Get("/api/orders?page=2&size=1"));

        Assert.Equal("ORD-PAGE-2", body.GetProperty("orders")[0].GetProperty("orderNumber").GetString());
        Assert.Equal(3, body.GetProperty("totalPages").GetInt32());
    }

    [Fact]
    public async Task CouponsPageNewestFirst()
    {
        var body = await Json(await Get("/api/coupons?page=1&size=1"));

        Assert.Equal("PAGE-3", body.GetProperty("coupons")[0].GetProperty("code").GetString());
    }

    [Theory]
    [InlineData("/api/orders?size=101", "size", "Page size must be between 1 and 100")]
    [InlineData("/api/orders?page=0", "page", "Page must be 1 or greater")]
    [InlineData("/api/coupons?size=0", "size", "Page size must be between 1 and 100")]
    [InlineData("/api/coupons?page=0", "page", "Page must be 1 or greater")]
    public async Task InvalidPagingIsAFieldError(string path, string field, string message)
    {
        var response = await Get(path);

        Assert.Equal(HttpStatusCode.UnprocessableEntity, response.StatusCode);
        var error = (await Json(response)).GetProperty("errors")[0];
        Assert.Equal(field, error.GetProperty("field").GetString());
        Assert.Equal(message, error.GetProperty("message").GetString());
    }

    private static string Fingerprint(string sku, string quantity, string country, string couponCode) =>
        Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes($"{sku}\n{quantity}\n{country}\n{couponCode}")))
            .ToLowerInvariant();

    private static Order NewOrder(string orderNumber, DateTime orderTimestamp) => new()
    {
        OrderNumber = orderNumber,
        OrderTimestamp = orderTimestamp,
        Country = "US",
        Sku = "BOOK-123",
        Quantity = 1,
        UnitPrice = 10m,
        BasePrice = 10m,
        DiscountRate = 0m,
        DiscountAmount = 0m,
        SubtotalPrice = 10m,
        TaxRate = 0.10m,
        TaxAmount = 1m,
        TotalPrice = 11m,
        Status = OrderStatus.PLACED
    };
}
