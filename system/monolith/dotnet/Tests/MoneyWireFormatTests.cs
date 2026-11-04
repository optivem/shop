using System.Net;
using System.Net.Http.Headers;
using System.Text.Json;
using Microsoft.Extensions.DependencyInjection;
using MyCompany.MyShop.Monolith.Core.Entities;
using MyCompany.MyShop.Monolith.Data;
using Xunit;

namespace MyCompany.MyShop.Monolith.Tests;

/// <summary>
/// Pins the wire shape of money and rates: every amount is a JSON string at 2 decimal places and every
/// rate a JSON string at 4, whatever scale the stored value happens to carry (the in-memory database
/// keeps none, so <c>20m</c> must still go out as <c>"20.00"</c>).
/// </summary>
public class MoneyWireFormatTests : IClassFixture<ShopWebApplicationFactory>
{
    // ShopWebApplicationFactory mints tokens for this subject.
    private const string Owner = "test-user";
    private const string OrderNumber = "ORD-MONEY-WIRE";
    private const string CouponCode = "MONEY-WIRE";

    private readonly ShopWebApplicationFactory _factory;

    public MoneyWireFormatTests(ShopWebApplicationFactory factory) => _factory = factory;

    private async Task Seed()
    {
        using var scope = _factory.Services.CreateScope();
        var dbContext = scope.ServiceProvider.GetRequiredService<AppDbContext>();
        // The in-memory database is shared with the other test classes, so seed only what is missing.
        if (!dbContext.Orders.Any(o => o.OrderNumber == OrderNumber))
        {
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
                OwnerName = "tester"
            });
        }
        if (!dbContext.Coupons.Any(c => c.Code == CouponCode))
        {
            dbContext.Coupons.Add(new Coupon { Code = CouponCode, DiscountRate = 0.15m, UsedCount = 0 });
        }
        await dbContext.SaveChangesAsync();
    }

    private async Task<JsonElement> GetJson(string path, string role)
    {
        await Seed();
        var client = _factory.CreateClient(new() { AllowAutoRedirect = false });
        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", _factory.CreateToken(role));
        var response = await client.GetAsync(path);
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
        var order = await GetJson($"/api/orders/{OrderNumber}", "CUSTOMER");

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
        var history = await GetJson("/api/orders", "CUSTOMER");

        var order = history.GetProperty("orders").EnumerateArray()
            .Single(o => o.GetProperty("orderNumber").GetString() == OrderNumber);
        AssertWireString(order, "totalPrice", "18.00");
    }

    [Fact]
    public async Task BrowseCoupons_WritesDiscountRateAsFixedScaleString()
    {
        var coupons = await GetJson("/api/coupons", "ADMIN");

        var coupon = coupons.GetProperty("coupons").EnumerateArray()
            .Single(c => c.GetProperty("code").GetString() == CouponCode);
        AssertWireString(coupon, "discountRate", "0.1500");
    }
}
