using System.Net;
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

namespace MyCompany.MyShop.Monolith.Tests;

/// <summary>
/// The service half of the shipping address, the admin customer filter and the profile: the real services
/// over an in-memory database, with the ERP, tax and clock systems answered by a stub HTTP handler.
/// </summary>
public class OrderShippingAddressAndProfileServiceTests : IDisposable
{
    private static readonly CurrentUser Ana = new("sub-ana", "ana", false, "ana@example.com", "Ana", "Novak");
    private static readonly CurrentUser Bob = new("sub-bob", "bob", false, "bob@example.com", "Bob", "Smith");
    private static readonly CurrentUser Admin = new("sub-admin", "admin1", true);

    private readonly AppDbContext _dbContext;
    private readonly OrderService _orderService;
    private readonly ProfileService _profileService;

    public OrderShippingAddressAndProfileServiceTests()
    {
        _dbContext = new AppDbContext(new DbContextOptionsBuilder<AppDbContext>()
            .UseInMemoryDatabase($"test-{Guid.NewGuid()}")
            .ConfigureWarnings(w => w.Ignore(InMemoryEventId.TransactionIgnoredWarning))
            .Options);

        var externalSystems = new StubErpHandler()
            .Returns("/api/products/BOOK-123", HttpStatusCode.OK, "{\"id\":\"BOOK-123\",\"price\":10.00}")
            .Returns("/api/promotion", HttpStatusCode.OK, "{\"promotionActive\":false,\"discount\":1.0}")
            .Returns("/api/time", HttpStatusCode.OK, "{\"time\":\"2025-06-15T10:00:00Z\"}")
            .Returns("/api/countries/US", HttpStatusCode.OK, "{\"id\":\"US\",\"countryName\":\"USA\",\"taxRate\":0.10}")
            .Returns("/api/countries/DE", HttpStatusCode.OK, "{\"id\":\"DE\",\"countryName\":\"Germany\",\"taxRate\":0.19}")
            .Returns("/api/countries/GB", HttpStatusCode.OK, "{\"id\":\"GB\",\"countryName\":\"UK\",\"taxRate\":0.20}");
        var http = new HttpClient(externalSystems);
        var configuration = new ConfigurationBuilder().Build();
        var clock = new ClockGateway(http, configuration);

        _orderService = new OrderService(_dbContext, new ErpGateway(http, configuration), new TaxGateway(http, configuration),
            clock, new CouponService(_dbContext, clock));
        _profileService = new ProfileService(_dbContext);
    }

    public void Dispose()
    {
        _dbContext.Dispose();
        GC.SuppressFinalize(this);
    }

    [Fact]
    public async Task PlaceOrder_WithShippingAddressChargesTaxOfTheAddressCountryAndStoresTheTrimmedAddress()
    {
        var request = Request("US");
        request.ShippingAddress = new ShippingAddress
        {
            FirstName = " Ana ", LastName = "Novak", Street = "Main St 1 ", PostalCode = "10115", City = "Berlin", Country = " DE"
        };

        var response = await _orderService.PlaceOrderAsync(request, Ana);

        var saved = await _dbContext.Orders.SingleAsync();
        Assert.Equal("DE", saved.Country);
        Assert.Equal(0.19m, saved.TaxRate);
        Assert.Equal(3.80m, saved.TaxAmount);
        Assert.Equal(23.80m, saved.TotalPrice);
        Assert.Equal(
            "{\"firstName\":\"Ana\",\"lastName\":\"Novak\",\"street\":\"Main St 1\",\"postalCode\":\"10115\",\"city\":\"Berlin\",\"country\":\"DE\"}",
            saved.ShippingAddress);

        var details = await _orderService.GetOrderAsync(response.OrderNumber, Ana);
        Assert.Equal("Ana", details.ShippingAddress!.FirstName);
        Assert.Equal("Main St 1", details.ShippingAddress.Street);
        Assert.Equal("DE", details.ShippingAddress.Country);
    }

    [Fact]
    public async Task PlaceOrder_WithShippingAddressToUnknownCountryIsAShippingAddressCountryError()
    {
        var request = Request("US");
        request.ShippingAddress = Address("XX");

        var ex = await Assert.ThrowsAsync<ValidationException>(() => _orderService.PlaceOrderAsync(request, Ana));

        Assert.Equal("shippingAddress.country", ex.FieldName);
        Assert.Equal("Country does not exist: XX", ex.Message);
    }

    [Fact]
    public async Task PlaceOrder_QuickOrderHasNoShippingAddress()
    {
        var response = await _orderService.PlaceOrderAsync(Request("US"), Ana);

        Assert.Null((await _dbContext.Orders.SingleAsync()).ShippingAddress);
        Assert.Null((await _orderService.GetOrderAsync(response.OrderNumber, Ana)).ShippingAddress);
    }

    [Fact]
    public async Task PlaceOrder_SameKeyWithADifferentShippingAddressIsRejected()
    {
        var first = Request("US");
        first.ShippingAddress = Address("DE");
        await _orderService.PlaceOrderAsync(first, Ana, "key-1");

        var second = Request("US");
        second.ShippingAddress = Address("GB");
        var ex = await Assert.ThrowsAsync<ValidationException>(() => _orderService.PlaceOrderAsync(second, Ana, "key-1"));

        Assert.Equal("Idempotency-Key has already been used with a different request", ex.Message);
    }

    [Fact]
    public async Task BrowseOrderHistory_AdminCustomerFilterShowsOnlyThatCustomersOrders()
    {
        await _orderService.PlaceOrderAsync(Request("US"), Ana);
        await _orderService.PlaceOrderAsync(Request("US"), Bob);

        var response = await _orderService.BrowseOrderHistoryAsync(null, Admin, customerFilter: " ana ");

        Assert.Equal(["ana"], response.Orders.Select(o => o.Customer));
        Assert.Equal(1, response.TotalElements);
        Assert.Equal(1, response.TotalPages);
    }

    [Fact]
    public async Task BrowseOrderHistory_BlankCustomerFilterIsNoFilter()
    {
        await _orderService.PlaceOrderAsync(Request("US"), Ana);
        await _orderService.PlaceOrderAsync(Request("US"), Bob);

        var response = await _orderService.BrowseOrderHistoryAsync(null, Admin, customerFilter: "  ");

        Assert.Equal(2, response.TotalElements);
    }

    [Fact]
    public async Task BrowseOrderHistory_CustomerFilterIsIgnoredForACustomer()
    {
        await _orderService.PlaceOrderAsync(Request("US"), Ana);
        await _orderService.PlaceOrderAsync(Request("US"), Bob);

        var response = await _orderService.BrowseOrderHistoryAsync(null, Ana, customerFilter: "bob");

        Assert.Equal(["ana"], response.Orders.Select(o => o.Customer));
    }

    [Fact]
    public async Task ViewProfile_WithoutSavedProfileTakesNamesAndEmailFromTheAccount()
    {
        var profile = await _profileService.ViewProfileAsync(Ana);

        Assert.Equal("Ana", profile.FirstName);
        Assert.Equal("Novak", profile.LastName);
        Assert.Equal("ana@example.com", profile.Email);
        Assert.Null(profile.Street);
        Assert.Null(profile.PostalCode);
        Assert.Null(profile.City);
        Assert.Null(profile.Country);
    }

    [Fact]
    public async Task UpdateProfile_SavesTrimmedValuesAndReplacesTheSavedProfile()
    {
        await _profileService.UpdateProfileAsync(Profile("Main St 1", "DE"), Ana);

        var saved = await _profileService.UpdateProfileAsync(Profile(" 1 High St ", "GB "), Ana);

        Assert.Equal("1 High St", saved.Street);
        Assert.Equal("GB", saved.Country);
        Assert.Equal("ana@example.com", saved.Email);
        var row = await _dbContext.CustomerProfiles.SingleAsync();
        Assert.Equal("sub-ana", row.Owner);
        Assert.Equal("1 High St", row.Street);
    }

    [Fact]
    public async Task ViewProfile_ShowsOnlyTheCallersOwnProfile()
    {
        await _profileService.UpdateProfileAsync(Profile("Private Lane 7", "DE"), Ana);

        var bobs = await _profileService.ViewProfileAsync(Bob);

        Assert.Null(bobs.Street);
        Assert.Equal("Bob", bobs.FirstName);
    }

    private static PlaceOrderRequest Request(string country) => new() { Sku = "BOOK-123", Quantity = 2, Country = country };

    private static ShippingAddress Address(string country) => new()
    {
        FirstName = "Ana", LastName = "Novak", Street = "Main St 1", PostalCode = "10115", City = "Berlin", Country = country
    };

    private static UpdateProfileRequest Profile(string street, string country) => new()
    {
        FirstName = "Ana", LastName = "Novak", Street = street, PostalCode = "10115", City = "Berlin", Country = country
    };
}
