using System.Net;
using System.Net.Http.Headers;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using MyCompany.MyShop.Backend.Core.Entities;
using MyCompany.MyShop.Backend.Data;
using MyCompany.MyShop.Backend.Tests.Support;
using Xunit;

namespace MyCompany.MyShop.Backend.Tests.Component;

/// <summary>
/// Who may see and change which order: customers see only their own orders (another customer's order
/// is reported as non-existent), admins see and cancel every order, and an order with no owner is
/// visible to admins only.
/// </summary>
public class OrderOwnershipComponentTests : IClassFixture<WebApplicationFactory<Program>>
{
    private const string OwnerSubject = "customer-user";
    private const string OwnedOrder = "ORD-OWNED";
    private const string UnownedOrder = "ORD-UNOWNED";

    private readonly WebApplicationFactory<Program> _factory;

    public OrderOwnershipComponentTests(WebApplicationFactory<Program> factory)
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
                    options.UseInMemoryDatabase("OrderOwnershipTestDb"));
            });
        });
    }

    private static string OwnerToken => TestAuth.CustomerTokenFor(OwnerSubject);

    private static string OtherCustomerToken => TestAuth.CustomerTokenFor("another-customer-user");

    private async Task SeedOrders()
    {
        using var scope = _factory.Services.CreateScope();
        var dbContext = scope.ServiceProvider.GetRequiredService<AppDbContext>();
        dbContext.Orders.RemoveRange(dbContext.Orders);
        dbContext.Orders.Add(NewOrder(OwnedOrder, OwnerSubject, OwnerSubject));
        dbContext.Orders.Add(NewOrder(UnownedOrder, null, null));
        await dbContext.SaveChangesAsync();
    }

    private async Task<HttpResponseMessage> Call(HttpMethod method, string path, string token)
    {
        await SeedOrders();
        var request = new HttpRequestMessage(method, path);
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);
        if (method == HttpMethod.Post)
            request.Content = new StringContent("{}", System.Text.Encoding.UTF8, "application/json");
        return await _factory.CreateClient().SendAsync(request);
    }

    [Fact]
    public async Task OwnerCanViewOwnOrder()
    {
        var response = await Call(HttpMethod.Get, $"/api/orders/{OwnedOrder}", OwnerToken);

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
    }

    [Fact]
    public async Task AnotherCustomerCannotViewOrder()
    {
        var response = await Call(HttpMethod.Get, $"/api/orders/{OwnedOrder}", OtherCustomerToken);

        Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
    }

    [Fact]
    public async Task AdminCanViewAnyOrder()
    {
        var response = await Call(HttpMethod.Get, $"/api/orders/{OwnedOrder}", TestAuth.AdminToken);

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
    }

    [Fact]
    public async Task CustomerCannotViewOrderWithoutOwner()
    {
        var response = await Call(HttpMethod.Get, $"/api/orders/{UnownedOrder}", OwnerToken);

        Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
    }

    [Fact]
    public async Task CustomerHistoryListsOnlyOwnOrders()
    {
        var response = await Call(HttpMethod.Get, "/api/orders", OwnerToken);

        var body = await response.Content.ReadAsStringAsync();
        Assert.Contains(OwnedOrder, body);
        Assert.DoesNotContain(UnownedOrder, body);
    }

    [Fact]
    public async Task AnotherCustomerHistoryDoesNotListOrder()
    {
        var response = await Call(HttpMethod.Get, "/api/orders", OtherCustomerToken);

        var body = await response.Content.ReadAsStringAsync();
        Assert.DoesNotContain(OwnedOrder, body);
        Assert.DoesNotContain(UnownedOrder, body);
    }

    [Fact]
    public async Task AdminHistoryListsAllOrdersWithCustomer()
    {
        var response = await Call(HttpMethod.Get, "/api/orders", TestAuth.AdminToken);

        var body = await response.Content.ReadAsStringAsync();
        Assert.Contains(OwnedOrder, body);
        Assert.Contains(UnownedOrder, body);
        Assert.Contains($"\"customer\":\"{OwnerSubject}\"", body);
    }

    [Fact]
    public async Task AnotherCustomerCannotCancelOrder()
    {
        var response = await Call(HttpMethod.Post, $"/api/orders/{OwnedOrder}/cancel", OtherCustomerToken);

        Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
        using var scope = _factory.Services.CreateScope();
        var dbContext = scope.ServiceProvider.GetRequiredService<AppDbContext>();
        var order = await dbContext.Orders.SingleAsync(o => o.OrderNumber == OwnedOrder);
        Assert.Equal(OrderStatus.PLACED, order.Status);
    }

    [Fact]
    public async Task OwnerCanCancelOwnOrder()
    {
        var response = await Call(HttpMethod.Post, $"/api/orders/{OwnedOrder}/cancel", OwnerToken);

        Assert.Equal(HttpStatusCode.NoContent, response.StatusCode);
    }

    [Fact]
    public async Task AdminCanCancelAnyOrder()
    {
        var response = await Call(HttpMethod.Post, $"/api/orders/{OwnedOrder}/cancel", TestAuth.AdminToken);

        Assert.Equal(HttpStatusCode.NoContent, response.StatusCode);
    }

    [Fact]
    public async Task AdminCannotPlaceOrder()
    {
        var response = await Call(HttpMethod.Post, "/api/orders", TestAuth.AdminToken);

        Assert.Equal(HttpStatusCode.Forbidden, response.StatusCode);
    }

    private static Order NewOrder(string orderNumber, string? owner, string? ownerName) => new()
    {
        OrderNumber = orderNumber,
        OrderTimestamp = new DateTime(2026, 3, 10, 12, 0, 0, DateTimeKind.Utc),
        Country = "US",
        Sku = "BOOK-123",
        Quantity = 2,
        UnitPrice = 10m,
        BasePrice = 20m,
        DiscountRate = 0m,
        DiscountAmount = 0m,
        SubtotalPrice = 20m,
        TaxRate = 0.10m,
        TaxAmount = 2m,
        TotalPrice = 22m,
        Status = OrderStatus.PLACED,
        Owner = owner,
        OwnerName = ownerName
    };
}
