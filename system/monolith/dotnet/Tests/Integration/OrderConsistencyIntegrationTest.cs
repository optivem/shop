using System.Net;
using System.Text;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Diagnostics;
using Microsoft.Extensions.Configuration;
using MyCompany.MyShop.Monolith.Core.Dtos;
using MyCompany.MyShop.Monolith.Core.Entities;
using MyCompany.MyShop.Monolith.Core.Exceptions;
using MyCompany.MyShop.Monolith.Core.Services;
using MyCompany.MyShop.Monolith.Core.Services.External;
using MyCompany.MyShop.Monolith.Data;
using Xunit;

namespace MyCompany.MyShop.Monolith.Tests.Integration;

[Trait("Category", "Integration")]
public class OrderConsistencyIntegrationTest : AbstractIntegrationTest
{
    private static readonly CurrentUser Customer = new("customer-sub", "customer1", false);

    private OrderService NewOrderService(AppDbContext db)
    {
        var http = new HttpClient(new StubHandler());
        var config = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?> { ["External:SystemMode"] = "real" })
            .Build();
        var clock = new ClockGateway(http, config);
        return new OrderService(db, new ErpGateway(http, config), new TaxGateway(http, config), clock,
            new CouponService(db, clock));
    }

    private static PlaceOrderRequest Request(string? couponCode) =>
        new() { Sku = "BOOK-123", Quantity = 1, Country = "US", CouponCode = couponCode };

    private async Task CreateCoupon(string code, int limit)
    {
        await new CouponService(DbContext, new ClockGateway(new HttpClient(), new ConfigurationBuilder().Build()))
            .CreateCouponAsync(code, 0.1m, null, null, limit);
    }

    [Fact]
    public async Task FailureWhileSavingOrderRollsBackCouponIncrement()
    {
        await CreateCoupon("SAVE10", 5);
        await using var failing = NewDbContext(new FailOnOrderInsert());

        await Assert.ThrowsAnyAsync<Exception>(() => NewOrderService(failing).PlaceOrderAsync(Request("SAVE10"), Customer));

        Assert.Equal(0, await DbContext.Orders.CountAsync());
        Assert.Equal(0, (await DbContext.Coupons.AsNoTracking().SingleAsync(c => c.Code == "SAVE10")).UsedCount);
    }

    [Fact]
    public async Task ConcurrentOrdersNeverExceedCouponUsageLimit()
    {
        await CreateCoupon("ONCE", 1);

        var results = await Task.WhenAll(Enumerable.Range(0, 8).Select(async _ =>
        {
            await using var db = NewDbContext();
            try
            {
                await NewOrderService(db).PlaceOrderAsync(Request("ONCE"), Customer);
                return true;
            }
            catch (ValidationException)
            {
                return false;
            }
        }));

        Assert.Equal(1, results.Count(r => r));
        Assert.Equal(1, await DbContext.Orders.CountAsync());
        Assert.Equal(1, (await DbContext.Coupons.AsNoTracking().SingleAsync(c => c.Code == "ONCE")).UsedCount);
    }

    [Fact]
    public async Task ConcurrentUpdatesToSameOrderConflict()
    {
        var number = (await NewOrderService(DbContext).PlaceOrderAsync(Request(null), Customer)).OrderNumber;
        await using var first = NewDbContext();
        await using var second = NewDbContext();
        var a = await first.Orders.SingleAsync(o => o.OrderNumber == number);
        var b = await second.Orders.SingleAsync(o => o.OrderNumber == number);

        a.Status = OrderStatus.DELIVERED;
        await first.SaveChangesAsync();
        b.Status = OrderStatus.CANCELLED;

        await Assert.ThrowsAsync<DbUpdateConcurrencyException>(() => second.SaveChangesAsync());
        await using var check = NewDbContext();
        Assert.Equal(OrderStatus.DELIVERED, (await check.Orders.SingleAsync(o => o.OrderNumber == number)).Status);
    }

    private sealed class FailOnOrderInsert : SaveChangesInterceptor
    {
        public override ValueTask<InterceptionResult<int>> SavingChangesAsync(
            DbContextEventData eventData, InterceptionResult<int> result, CancellationToken cancellationToken = default)
        {
            if (eventData.Context!.ChangeTracker.Entries<Order>().Any(e => e.State == EntityState.Added))
            {
                throw new InvalidOperationException("boom");
            }

            return base.SavingChangesAsync(eventData, result, cancellationToken);
        }
    }

    private sealed class StubHandler : HttpMessageHandler
    {
        protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
        {
            var path = request.RequestUri!.AbsolutePath;
            var json = path.EndsWith("/promotion") ? "{\"promotionActive\":false,\"discount\":1.0}"
                : path.Contains("/products/") ? "{\"id\":\"BOOK-123\",\"price\":10.00}"
                : "{\"id\":\"US\",\"countryName\":\"US\",\"taxRate\":0.10}";
            return Task.FromResult(new HttpResponseMessage(HttpStatusCode.OK)
            {
                Content = new StringContent(json, Encoding.UTF8, "application/json")
            });
        }
    }
}
