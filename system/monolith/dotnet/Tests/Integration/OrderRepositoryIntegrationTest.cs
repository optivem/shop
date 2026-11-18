using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using MyCompany.MyShop.Monolith.Core.Entities;
using MyCompany.MyShop.Monolith.Core.Services;
using MyCompany.MyShop.Monolith.Core.Services.External;
using Xunit;

namespace MyCompany.MyShop.Monolith.Tests.Integration;

[Trait("Category", "Integration")]
public class OrderRepositoryIntegrationTest : AbstractIntegrationTest
{
    [Fact]
    public async Task SavesAndReadsBackOrder()
    {
        var order = new Order
        {
            OrderNumber = "ORD-001",
            OrderTimestamp = new DateTime(2026, 1, 1, 0, 0, 0, DateTimeKind.Utc),
            Country = "US",
            Sku = "BOOK-123",
            Quantity = 2,
            UnitPrice = 10.00m,
            BasePrice = 20.00m,
            DiscountRate = 0.0000m,
            DiscountAmount = 0.00m,
            SubtotalPrice = 20.00m,
            TaxRate = 0.1000m,
            TaxAmount = 2.00m,
            TotalPrice = 22.00m,
            Status = OrderStatus.PLACED,
            AppliedCouponCode = null
        };

        DbContext.Orders.Add(order);
        await DbContext.SaveChangesAsync();

        DbContext.ChangeTracker.Clear();

        var found = await DbContext.Orders.FirstOrDefaultAsync(o => o.OrderNumber == "ORD-001");
        Assert.NotNull(found);
        Assert.Equal("BOOK-123", found.Sku);
        Assert.Equal(22.00m, found.TotalPrice);
        Assert.Equal(OrderStatus.PLACED, found.Status);
    }

    // The unpaged customer list the admin filter offers: SELECT DISTINCT ... ORDER BY against real Postgres.
    [Fact]
    public async Task CustomersAreListedOnceEachFromAToZ()
    {
        DbContext.Orders.AddRange(
            OwnedBy("ORD-001", "carol"),
            OwnedBy("ORD-002", "alice"),
            OwnedBy("ORD-003", "carol"),
            OwnedBy("ORD-004", null));
        await DbContext.SaveChangesAsync();
        DbContext.ChangeTracker.Clear();

        var http = new HttpClient();
        var config = new ConfigurationBuilder().Build();
        var clock = new ClockGateway(http, config);
        var service = new OrderService(DbContext, new ErpGateway(http, config), new TaxGateway(http, config), clock,
            new CouponService(DbContext, clock));

        Assert.Equal(["alice", "carol"], await service.BrowseCustomersAsync());
    }

    private static Order OwnedBy(string orderNumber, string? ownerName) => new()
    {
        OrderNumber = orderNumber,
        OrderTimestamp = new DateTime(2026, 1, 1, 0, 0, 0, DateTimeKind.Utc),
        Country = "US",
        Sku = "BOOK-123",
        Quantity = 1,
        UnitPrice = 10.00m,
        BasePrice = 10.00m,
        DiscountRate = 0.0000m,
        DiscountAmount = 0.00m,
        SubtotalPrice = 10.00m,
        TaxRate = 0.1000m,
        TaxAmount = 1.00m,
        TotalPrice = 11.00m,
        Status = OrderStatus.PLACED,
        Owner = ownerName == null ? null : ownerName + "-sub",
        OwnerName = ownerName
    };
}
