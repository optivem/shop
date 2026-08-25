using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Moq;
using MyCompany.MyShop.Backend.Core.Dtos;
using MyCompany.MyShop.Backend.Core.Dtos.External;
using MyCompany.MyShop.Backend.Core.Entities;
using MyCompany.MyShop.Backend.Core.Exceptions;
using MyCompany.MyShop.Backend.Core.Services;
using MyCompany.MyShop.Backend.Core.Services.External;
using MyCompany.MyShop.Backend.Data;
using Xunit;

namespace MyCompany.MyShop.Backend.Tests.Unit;

public class OrderServiceTest : IDisposable
{
    private static readonly DateTime NormalTime = new(2025, 6, 15, 10, 0, 0, DateTimeKind.Utc);
    private static readonly DateTime Dec31YearEndBlackout = new(2025, 12, 31, 23, 59, 0, DateTimeKind.Utc);
    private static readonly DateTime Dec31CancelBlackout = new(2025, 12, 31, 22, 15, 0, DateTimeKind.Utc);

    private readonly AppDbContext _dbContext;
    private readonly Mock<ClockGateway> _clockMock;
    private readonly Mock<ErpGateway> _erpMock;
    private readonly Mock<TaxGateway> _taxMock;
    private readonly Mock<CouponService> _couponMock;
    private readonly OrderService _service;

    public OrderServiceTest()
    {
        var config = new Mock<IConfiguration>();
        var httpClient = new HttpClient();

        _dbContext = new AppDbContext(new DbContextOptionsBuilder<AppDbContext>()
            .UseInMemoryDatabase($"test-{Guid.NewGuid()}")
            .Options);

        _clockMock = new Mock<ClockGateway>(httpClient, config.Object);
        _erpMock = new Mock<ErpGateway>(httpClient, config.Object);
        _taxMock = new Mock<TaxGateway>(httpClient, config.Object);
        _couponMock = new Mock<CouponService>(_dbContext, _clockMock.Object);

        _service = new OrderService(_dbContext, _erpMock.Object, _taxMock.Object, _clockMock.Object, _couponMock.Object);
    }

    public void Dispose()
    {
        _dbContext.Dispose();
        GC.SuppressFinalize(this);
    }

    [Fact]
    public async Task PlaceOrder_ReturnsOrderNumberStartingWithOrd()
    {
        GivenNormalTime();
        GivenProductExists("BOOK-123", 10.00m);
        GivenNoPromotion();
        GivenNoDiscount();
        GivenTaxRate("US", 0.10m);

        var response = await _service.PlaceOrderAsync(BuildRequest("BOOK-123", 2, "US"));

        Assert.StartsWith("ORD-", response.OrderNumber);
        await AssertSavedOrder(response);
    }

    [Fact]
    public async Task PlaceOrder_ThrowsWhenOrderedOnYearEndBlackout()
    {
        _clockMock.Setup(g => g.GetCurrentTimeAsync()).ReturnsAsync(Dec31YearEndBlackout);

        var ex = await Assert.ThrowsAsync<ValidationException>(
            () => _service.PlaceOrderAsync(BuildRequest("BOOK-123", 1, "US")));

        Assert.Contains("December 31", ex.Message);
    }

    [Fact]
    public async Task PlaceOrder_ThrowsWhenSkuUnknown()
    {
        GivenNormalTime();
        _erpMock.Setup(g => g.GetProductDetailsAsync("UNKNOWN")).ReturnsAsync((ErpProductDetailsResponse?)null);

        var ex = await Assert.ThrowsAsync<ValidationException>(
            () => _service.PlaceOrderAsync(BuildRequest("UNKNOWN", 1, "US")));

        Assert.Equal("sku", ex.FieldName);
    }

    [Fact]
    public async Task PlaceOrder_ThrowsWhenCountryUnknown()
    {
        GivenNormalTime();
        GivenProductExists("BOOK-123", 10.00m);
        GivenNoPromotion();
        GivenNoDiscount();
        _taxMock.Setup(g => g.GetTaxDetailsAsync("XX")).ReturnsAsync((TaxDetailsResponse?)null);

        var ex = await Assert.ThrowsAsync<ValidationException>(
            () => _service.PlaceOrderAsync(BuildRequest("BOOK-123", 1, "XX")));

        Assert.Equal("country", ex.FieldName);
    }

    [Fact]
    public async Task DeliverOrder_TransitionsStatusToDelivered()
    {
        await SeedOrder("ORD-001", OrderStatus.PLACED);

        await _service.DeliverOrderAsync("ORD-001");

        var saved = await _dbContext.Orders.FirstAsync(o => o.OrderNumber == "ORD-001");
        Assert.Equal(OrderStatus.DELIVERED, saved.Status);
    }

    [Fact]
    public async Task DeliverOrder_ThrowsWhenOrderNotFound()
    {
        await Assert.ThrowsAsync<NotExistValidationException>(
            () => _service.DeliverOrderAsync("ORD-999"));
    }

    [Fact]
    public async Task DeliverOrder_ThrowsWhenOrderAlreadyDelivered()
    {
        await SeedOrder("ORD-001", OrderStatus.DELIVERED);

        var ex = await Assert.ThrowsAsync<ValidationException>(
            () => _service.DeliverOrderAsync("ORD-001"));

        Assert.Contains("cannot be delivered", ex.Message);
    }

    [Fact]
    public async Task CancelOrder_TransitionsStatusToCancelled()
    {
        GivenNormalTime();
        await SeedOrder("ORD-001", OrderStatus.PLACED);

        await _service.CancelOrderAsync("ORD-001");

        var saved = await _dbContext.Orders.FirstAsync(o => o.OrderNumber == "ORD-001");
        Assert.Equal(OrderStatus.CANCELLED, saved.Status);
    }

    [Fact]
    public async Task CancelOrder_ThrowsDuringDecember31CancellationBlackout()
    {
        _clockMock.Setup(g => g.GetCurrentTimeAsync()).ReturnsAsync(Dec31CancelBlackout);

        var ex = await Assert.ThrowsAsync<ValidationException>(
            () => _service.CancelOrderAsync("ORD-001"));

        Assert.Contains("December 31", ex.Message);
    }

    [Fact]
    public async Task CancelOrder_ThrowsWhenOrderAlreadyCancelled()
    {
        GivenNormalTime();
        await SeedOrder("ORD-001", OrderStatus.CANCELLED);

        var ex = await Assert.ThrowsAsync<ValidationException>(
            () => _service.CancelOrderAsync("ORD-001"));

        Assert.Contains("already been cancelled", ex.Message);
    }

    private void GivenNormalTime() =>
        _clockMock.Setup(g => g.GetCurrentTimeAsync()).ReturnsAsync(NormalTime);

    private void GivenProductExists(string sku, decimal price) =>
        _erpMock.Setup(g => g.GetProductDetailsAsync(sku))
            .ReturnsAsync(new ErpProductDetailsResponse { Price = price });

    private void GivenNoPromotion() =>
        _erpMock.Setup(g => g.GetPromotionDetailsAsync())
            .ReturnsAsync(new ErpGetPromotionResponse { PromotionActive = false, Discount = 1m });

    private void GivenNoDiscount() =>
        _couponMock.Setup(s => s.GetDiscountAsync(null)).ReturnsAsync(0m);

    private void GivenTaxRate(string country, decimal rate) =>
        _taxMock.Setup(g => g.GetTaxDetailsAsync(country))
            .ReturnsAsync(new TaxDetailsResponse { TaxRate = rate });

    private static PlaceOrderRequest BuildRequest(string sku, int quantity, string country) =>
        new() { Sku = sku, Quantity = quantity, Country = country };

    private async Task SeedOrder(string orderNumber, OrderStatus status)
    {
        _dbContext.Orders.Add(new Order
        {
            OrderNumber = orderNumber,
            OrderTimestamp = NormalTime,
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
            Status = status
        });
        await _dbContext.SaveChangesAsync();
    }

    private async Task AssertSavedOrder(PlaceOrderResponse response)
    {
        var saved = await _dbContext.Orders.FirstAsync();
        Assert.StartsWith("ORD-", saved.OrderNumber);
        Assert.Equal(response.OrderNumber, saved.OrderNumber);
        Assert.Equal(NormalTime, saved.OrderTimestamp);
        Assert.Equal("BOOK-123", saved.Sku);
        Assert.Equal(2, saved.Quantity);
        Assert.Equal("US", saved.Country);
        Assert.Equal(10.00m, saved.UnitPrice);
        Assert.Equal(20.00m, saved.BasePrice);
        Assert.Equal(0m, saved.DiscountRate);
        Assert.Equal(0m, saved.DiscountAmount);
        Assert.Equal(20.00m, saved.SubtotalPrice);
        Assert.Equal(0.10m, saved.TaxRate);
        Assert.Equal(2.00m, saved.TaxAmount);
        Assert.Equal(22.00m, saved.TotalPrice);
        Assert.Equal(OrderStatus.PLACED, saved.Status);
        Assert.Null(saved.AppliedCouponCode);
    }
}
