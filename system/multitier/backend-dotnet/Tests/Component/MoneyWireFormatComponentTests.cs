using System.Net;
using System.Net.Http.Headers;
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
/// Pins the wire shape of money and rates: every amount is a JSON string at 2 decimal places and every
/// rate a JSON string at 4, whatever scale the stored value happens to carry (the in-memory database
/// keeps none, so <c>20m</c> must still go out as <c>"20.00"</c>).
/// </summary>
public class MoneyWireFormatComponentTests : IClassFixture<WebApplicationFactory<Program>>
{
    private const string Owner = "money-customer";
    private const string OrderNumber = "ORD-MONEY-WIRE";
    private const string CouponCode = "MONEY-WIRE";

    private readonly WebApplicationFactory<Program> _factory;

    public MoneyWireFormatComponentTests(WebApplicationFactory<Program> factory)
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
                    options.UseInMemoryDatabase("MoneyWireFormatTestDb"));
            });
        });
    }

    private async Task Seed()
    {
        using var scope = _factory.Services.CreateScope();
        var dbContext = scope.ServiceProvider.GetRequiredService<AppDbContext>();
        dbContext.Orders.RemoveRange(dbContext.Orders);
        dbContext.Coupons.RemoveRange(dbContext.Coupons);
        dbContext.Orders.Add(new Order
        {
            OrderNumber = OrderNumber,
            OrderTimestamp = new DateTime(2026, 3, 10, 12, 0, 0, DateTimeKind.Utc),
            Country = "US",
            Sku = "BOOK-123",
            Quantity = 2,
            UnitPrice = 10m,
            BasePrice = 20m,
            DiscountRate = 0.2m,
            DiscountAmount = 4m,
            SubtotalPrice = 16m,
            TaxRate = 0.125m,
            TaxAmount = 2m,
            TotalPrice = 18m,
            Status = OrderStatus.PLACED,
            Owner = Owner,
            OwnerName = Owner
        });
        dbContext.Coupons.Add(new Coupon { Code = CouponCode, DiscountRate = 0.15m, UsedCount = 0 });
        await dbContext.SaveChangesAsync();
    }

    private async Task<JsonElement> GetJson(string path, string token)
    {
        await Seed();
        var request = new HttpRequestMessage(HttpMethod.Get, path);
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);
        var response = await _factory.CreateClient().SendAsync(request);
        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        return JsonDocument.Parse(await response.Content.ReadAsStringAsync()).RootElement;
    }

    private static void AssertWireString(JsonElement element, string property, string expected)
    {
        var value = element.GetProperty(property);
        Assert.Equal(JsonValueKind.String, value.ValueKind);
        Assert.Equal(expected, value.GetString());
    }

    [Fact]
    public async Task ViewOrder_WritesMoneyAndRatesAsFixedScaleStrings()
    {
        var order = await GetJson($"/api/orders/{OrderNumber}", TestAuth.CustomerTokenFor(Owner));

        AssertWireString(order, "unitPrice", "10.00");
        AssertWireString(order, "basePrice", "20.00");
        AssertWireString(order, "discountRate", "0.2000");
        AssertWireString(order, "discountAmount", "4.00");
        AssertWireString(order, "subtotalPrice", "16.00");
        AssertWireString(order, "taxRate", "0.1250");
        AssertWireString(order, "taxAmount", "2.00");
        AssertWireString(order, "totalPrice", "18.00");
        Assert.Equal(JsonValueKind.Number, order.GetProperty("quantity").ValueKind);
    }

    [Fact]
    public async Task BrowseOrderHistory_WritesTotalPriceAsFixedScaleString()
    {
        var history = await GetJson("/api/orders", TestAuth.CustomerTokenFor(Owner));

        AssertWireString(history.GetProperty("orders")[0], "totalPrice", "18.00");
    }

    [Fact]
    public async Task BrowseCoupons_WritesDiscountRateAsFixedScaleString()
    {
        var coupons = await GetJson("/api/coupons", TestAuth.AdminToken);

        var coupon = coupons.GetProperty("coupons").EnumerateArray()
            .Single(c => c.GetProperty("code").GetString() == CouponCode);
        AssertWireString(coupon, "discountRate", "0.1500");
    }
}
