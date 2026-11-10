using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text.RegularExpressions;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Diagnostics;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using MyCompany.MyShop.Monolith.Core.Dtos;
using MyCompany.MyShop.Monolith.Core.Services.External;
using MyCompany.MyShop.Monolith.Data;
using Xunit;

namespace MyCompany.MyShop.Monolith.Tests;

/// <summary>
/// The server-rendered storefront over HTTP (storefront-ux §2–§4): the public catalog, product and cart pages, the
/// session cart, and the customer-only checkout, with the ERP, tax and clock systems answered by a stub HTTP handler.
/// Each test signs in as its own subject, so profiles and orders don't leak between tests.
/// </summary>
public partial class StorefrontPagesTests : IClassFixture<ShopWebApplicationFactory>
{
    private const string Catalog = """
        [{"id":"HP-15","title":"HP Laptop 15","description":"HP Laptop 15","brand":"HP","category":"Laptops","price":699.99},
         {"id":"DELL-XPS","title":"Dell XPS 13","description":"Dell XPS 13","brand":"Dell","category":"Laptops","price":1299.99},
         {"id":"LOGI-MX3S","title":"Logitech MX Master 3S","description":"Logitech MX Master 3S","brand":"Logitech","category":"Accessories","price":99.99}]
        """;

    private readonly ShopWebApplicationFactory _factory;

    public StorefrontPagesTests(ShopWebApplicationFactory factory) => _factory = factory;

    /// <summary>A browser-like client: it keeps cookies (the session cart) and does not follow redirects.</summary>
    private HttpClient Browser(StubErpHandler? externalSystems = null, string? role = null)
    {
        var stub = externalSystems ?? ExternalSystems();
        var factory = _factory.WithWebHostBuilder(builder => builder.ConfigureServices(services =>
        {
            services.AddScoped(sp => new ErpGateway(new HttpClient(stub), sp.GetRequiredService<IConfiguration>()));
            services.AddScoped(sp => new TaxGateway(new HttpClient(stub), sp.GetRequiredService<IConfiguration>()));
            services.AddScoped(sp => new ClockGateway(new HttpClient(stub), sp.GetRequiredService<IConfiguration>()));
            // Placing an order opens a transaction, which the in-memory database only warns about.
            var options = services.Single(d => d.ServiceType == typeof(DbContextOptions<AppDbContext>));
            services.Remove(options);
            services.AddDbContext<AppDbContext>(db => db.UseInMemoryDatabase("StorefrontPagesTests")
                .ConfigureWarnings(w => w.Ignore(InMemoryEventId.TransactionIgnoredWarning)));
        }));
        var client = factory.CreateClient(new WebApplicationFactoryClientOptions { AllowAutoRedirect = false });
        if (role != null)
        {
            var token = _factory.CreateToken(role, subject: "sub-" + Guid.NewGuid());
            client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);
        }

        return client;
    }

    private static StubErpHandler ExternalSystems() => new StubErpHandler()
        .Returns("/api/products", HttpStatusCode.OK, Catalog)
        .Returns("/api/products/HP-15", HttpStatusCode.OK,
            """{"id":"HP-15","title":"HP Laptop 15","description":"HP Laptop 15","brand":"HP","category":"Laptops","price":699.99}""")
        .Returns("/api/products/DELL-XPS", HttpStatusCode.OK,
            """{"id":"DELL-XPS","title":"Dell XPS 13","description":"Dell XPS 13","brand":"Dell","category":"Laptops","price":1299.99}""")
        .Returns("/api/promotion", HttpStatusCode.OK, """{"promotionActive":false,"discount":1.0}""")
        .Returns("/api/time", HttpStatusCode.OK, """{"time":"2025-06-15T10:00:00Z"}""")
        .Returns("/api/countries/US", HttpStatusCode.OK, """{"id":"US","countryName":"USA","taxRate":0.00}""")
        .Returns("/api/countries/DE", HttpStatusCode.OK, """{"id":"DE","countryName":"Germany","taxRate":0.19}""")
        .Returns("/api/countries/GB", HttpStatusCode.OK, """{"id":"GB","countryName":"UK","taxRate":0.20}""");

    [Fact]
    public async Task AnonymousVisitorBrowsesTheCatalogAndIsOfferedToSignIn()
    {
        var client = Browser();

        var response = await client.GetAsync("/");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var html = await response.Content.ReadAsStringAsync();
        Assert.Contains("aria-label=\"Product DELL-XPS\"", html);
        Assert.Contains("$1,299.99", html);
        Assert.Contains("href=\"/sign-in?returnUrl=%2F\"", html);
        Assert.Contains("aria-label=\"Sign In\"", html);
        Assert.Contains("href=\"/?category=Accessories\"", html);
        Assert.DoesNotContain("href=\"/order-history\"", html);
        Assert.Equal(HttpStatusCode.OK, (await client.SendAsync(new HttpRequestMessage(HttpMethod.Head, "/"))).StatusCode);
    }

    [Theory]
    [InlineData("/products/HP-15")]
    [InlineData("/cart")]
    [InlineData("/css/storefront.css")]
    public async Task StorefrontPagesArePublic(string path)
    {
        var response = await Browser().GetAsync(path);

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
    }

    [Fact]
    public async Task SignedInCustomerSeesEachAccountLinkOnceOnTheCatalog()
    {
        var html = await (await Browser(role: "CUSTOMER").GetAsync("/")).Content.ReadAsStringAsync();

        Assert.Equal(1, Count(html, "href=\"/new-order\""));
        Assert.Equal(1, Count(html, "href=\"/order-history\""));
        Assert.Contains(">My orders<", html);
        Assert.Contains("data-testid=\"username\"", html);
        Assert.DoesNotContain("href=\"/admin-coupons\"", html);
        Assert.DoesNotContain("aria-label=\"Sign In\"", html);
    }

    [Fact]
    public async Task SignedInAdminSeesAllOrdersAndAdminButNoQuickOrder()
    {
        var html = await (await Browser(role: "ADMIN").GetAsync("/")).Content.ReadAsStringAsync();

        Assert.Contains(">All orders<", html);
        Assert.Equal(1, Count(html, "href=\"/admin-coupons\""));
        Assert.DoesNotContain("href=\"/new-order\"", html);
    }

    [Fact]
    public async Task CatalogFiltersSearchesAndSorts()
    {
        var html = await (await Browser().GetAsync("/?category=Laptops&sort=price-desc")).Content.ReadAsStringAsync();

        Assert.Contains("2 products", html);
        Assert.DoesNotContain("aria-label=\"Product LOGI-MX3S\"", html);
        Assert.True(html.IndexOf("Product DELL-XPS", StringComparison.Ordinal) < html.IndexOf("Product HP-15", StringComparison.Ordinal));

        var searched = await (await Browser().GetAsync("/?q=logitech")).Content.ReadAsStringAsync();
        Assert.Contains("1 product<", searched);
        Assert.Contains("value=\"logitech\"", searched);
    }

    [Fact]
    public async Task FilterFormDefaultsRedirectToTheShortAddress()
    {
        var response = await Browser().GetAsync("/?q=&category=Laptops&sort=featured");

        Assert.Equal(HttpStatusCode.Redirect, response.StatusCode);
        Assert.Equal("/?category=Laptops", response.Headers.Location!.ToString());
    }

    [Fact]
    public async Task CatalogTellsTheVisitorWhenTheErpIsUnavailable()
    {
        var erpDown = new StubErpHandler().Returns("/api/products", HttpStatusCode.ServiceUnavailable);

        var response = await Browser(erpDown).GetAsync("/");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        Assert.Contains("Products are temporarily unavailable.", await response.Content.ReadAsStringAsync());
    }

    [Fact]
    public async Task ProductPageShowsTheProductWithAQuantityToAdd()
    {
        var html = await (await Browser().GetAsync("/products/HP-15")).Content.ReadAsStringAsync();

        Assert.Contains("aria-label=\"Product Title\">HP Laptop 15<", html);
        Assert.Contains("aria-label=\"Product Quantity\"", html);
        Assert.Contains("$699.99", html);
    }

    [Fact]
    public async Task UnknownSkuIsAProductNotFoundPage()
    {
        var response = await Browser().GetAsync("/products/NO-SUCH-SKU");

        Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
        Assert.Contains("Product not found", await response.Content.ReadAsStringAsync());
    }

    [Fact]
    public async Task ProductPageTellsTheVisitorWhenTheErpIsUnavailable()
    {
        var erpDown = new StubErpHandler().Returns("/api/products/HP-15", HttpStatusCode.ServiceUnavailable);

        var response = await Browser(erpDown).GetAsync("/products/HP-15");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var html = await response.Content.ReadAsStringAsync();
        Assert.Contains("alert-danger", html);
        Assert.Contains("Products are temporarily unavailable.", html);
    }

    [Fact]
    public async Task AddingToTheCartGoesBackWithANoticeAndRaisesTheBadge()
    {
        var client = Browser();

        var response = await PostFormAsync(client, "/products/HP-15", "/cart/add",
            ("sku", "HP-15"), ("quantity", "2"), ("returnUrl", "/products/HP-15"));

        Assert.Equal(HttpStatusCode.Redirect, response.StatusCode);
        Assert.Equal("/products/HP-15", response.Headers.Location!.ToString());
        var cookie = Assert.Single(response.Headers.GetValues("Set-Cookie"), c => c.StartsWith("myshop.session=", StringComparison.Ordinal));
        Assert.Contains("httponly", cookie, StringComparison.OrdinalIgnoreCase);

        var landed = await (await client.GetAsync("/products/HP-15")).Content.ReadAsStringAsync();
        Assert.Contains("notification success", landed);
        Assert.Contains("Added to cart", landed);

        await PostFormAsync(client, "/", "/cart/add", ("sku", "HP-15"), ("quantity", "1"), ("returnUrl", "/"));
        var cart = await (await client.GetAsync("/cart")).Content.ReadAsStringAsync();
        Assert.Contains("aria-label=\"Cart Line HP-15\"", cart);
        Assert.Contains("aria-label=\"Cart Item Count\">3<", cart);
        Assert.Contains("$2,099.97", cart);
        Assert.DoesNotContain("myshop.session", cart);
    }

    [Fact]
    public async Task AnInvalidQuantityIsNotAdded()
    {
        var client = Browser();

        await PostFormAsync(client, "/products/HP-15", "/cart/add", ("sku", "HP-15"), ("quantity", "100"), ("returnUrl", "/products/HP-15"));

        var landed = await (await client.GetAsync("/products/HP-15")).Content.ReadAsStringAsync();
        Assert.Contains("Quantity must be a whole number from 1 to 99.", landed);
        Assert.DoesNotContain("Cart Item Count", landed);
    }

    [Fact]
    public async Task CartQuantityChangesAndRemovalRedirectBackToTheCart()
    {
        var client = Browser();
        await PostFormAsync(client, "/", "/cart/add", ("sku", "HP-15"), ("quantity", "1"), ("returnUrl", "/"));

        var updated = await PostFormAsync(client, "/cart", "/cart/update", ("sku", "HP-15"), ("quantity", "4"));
        Assert.Equal("/cart", updated.Headers.Location!.ToString());
        Assert.Contains("aria-label=\"Cart Item Count\">4<", await (await client.GetAsync("/cart")).Content.ReadAsStringAsync());

        await PostFormAsync(client, "/cart", "/cart/remove", ("sku", "HP-15"));
        Assert.Contains("Your cart is empty.", await (await client.GetAsync("/cart")).Content.ReadAsStringAsync());
    }

    [Fact]
    public async Task CartChangesNeedTheAntiforgeryToken()
    {
        var response = await Browser().PostAsync("/cart/add", new FormUrlEncodedContent([new("sku", "HP-15")]));

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task CheckoutWithAnEmptyCartGoesToTheCart()
    {
        var response = await Browser(role: "CUSTOMER").GetAsync("/checkout");

        Assert.Equal(HttpStatusCode.Redirect, response.StatusCode);
        Assert.Equal("/cart", response.Headers.Location!.ToString());
    }

    [Theory]
    [InlineData("/checkout")]
    [InlineData("/checkout/confirmation")]
    public async Task CheckoutIsForCustomersOnly(string path)
    {
        var response = await Browser(role: "ADMIN").GetAsync(path);

        Assert.Equal(HttpStatusCode.Forbidden, response.StatusCode);
    }

    [Fact]
    public async Task FirstCheckoutOffersTheAddressFormWithSaveAsDefault()
    {
        var client = Browser(role: "CUSTOMER");
        await PostFormAsync(client, "/", "/cart/add", ("sku", "HP-15"), ("quantity", "1"), ("returnUrl", "/"));

        var html = await (await client.GetAsync("/checkout")).Content.ReadAsStringAsync();

        Assert.Contains("aria-label=\"Shipping Street\"", html);
        Assert.Contains("aria-label=\"Save as Default Address\" checked=\"checked\"", html);
        Assert.DoesNotContain("aria-label=\"Use Saved Address\"", html);
    }

    [Fact]
    public async Task CheckoutPreselectsTheSavedAddressAndEstimatesItsTax()
    {
        var client = Browser(role: "CUSTOMER");
        await SaveProfileAsync(client);
        await PostFormAsync(client, "/", "/cart/add", ("sku", "HP-15"), ("quantity", "1"), ("returnUrl", "/"));

        var html = await (await client.GetAsync("/checkout")).Content.ReadAsStringAsync();

        Assert.Contains("Ana Novak, Main St 1, 10115 Berlin, DE", html);
        Assert.Contains("aria-label=\"Use Saved Address\" checked=\"checked\"", html);
        Assert.Contains(">19%<", html);
        Assert.DoesNotContain("aria-label=\"Save as Default Address\"", html);
    }

    [Fact]
    public async Task CheckoutPlacesOneOrderPerLineAndKeepsTheRejectedOnesInTheCart()
    {
        var client = Browser(role: "CUSTOMER");
        await PostFormAsync(client, "/", "/cart/add", ("sku", "HP-15"), ("quantity", "2"), ("returnUrl", "/"));
        await PostFormAsync(client, "/", "/cart/add", ("sku", "NO-SUCH-SKU"), ("quantity", "1"), ("returnUrl", "/"));

        var response = await PostFormAsync(client, "/checkout", "/checkout",
            ("addressChoice", "other"), ("firstName", "Ana"), ("lastName", "Novak"), ("street", "Main St 1"),
            ("postalCode", "10115"), ("city", "Berlin"), ("country", "DE"), ("saveAsDefault", "true"), ("couponCode", ""));

        Assert.Equal(HttpStatusCode.Redirect, response.StatusCode);
        Assert.Equal("/checkout/confirmation", response.Headers.Location!.ToString());
        var confirmation = await (await client.GetAsync("/checkout/confirmation")).Content.ReadAsStringAsync();
        Assert.Contains("Thank you for your order!", confirmation);
        var orderNumber = OrderLink().Match(confirmation).Groups[1].Value;
        Assert.Contains($"aria-label=\"Confirmed Order {orderNumber}\"", confirmation);
        Assert.Contains("aria-label=\"Rejection Reason NO-SUCH-SKU\">Product does not exist for SKU: NO-SUCH-SKU<", confirmation);

        var cart = await (await client.GetAsync("/cart")).Content.ReadAsStringAsync();
        Assert.Contains("aria-label=\"Cart Line NO-SUCH-SKU\"", cart);
        Assert.DoesNotContain("aria-label=\"Cart Line HP-15\"", cart);

        // "Save as default" on a first order stored the address, so the next checkout preselects it.
        var checkout = await (await client.GetAsync("/checkout")).Content.ReadAsStringAsync();
        Assert.Contains("Ana Novak, Main St 1, 10115 Berlin, DE", checkout);
    }

    [Fact]
    public async Task CheckoutPlacesNothingWhenEveryLineIsRejected()
    {
        var client = Browser(role: "CUSTOMER");
        await SaveProfileAsync(client);
        await PostFormAsync(client, "/", "/cart/add", ("sku", "HP-15"), ("quantity", "1"), ("returnUrl", "/"));

        var response = await PostFormAsync(client, "/checkout", "/checkout", ("addressChoice", "saved"), ("couponCode", "INVALIDCOUPON"));

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var html = await response.Content.ReadAsStringAsync();
        Assert.Contains("No order was placed.", html);
        Assert.Contains("HP-15: Coupon code INVALIDCOUPON does not exist", html);
        Assert.Contains("aria-label=\"Cart Line HP-15\"", await (await client.GetAsync("/cart")).Content.ReadAsStringAsync());
    }

    [Fact]
    public async Task CheckoutRejectsAnIncompleteAddressWithoutPlacingAnOrder()
    {
        var client = Browser(role: "CUSTOMER");
        await PostFormAsync(client, "/", "/cart/add", ("sku", "HP-15"), ("quantity", "1"), ("returnUrl", "/"));

        var response = await PostFormAsync(client, "/checkout", "/checkout",
            ("addressChoice", "other"), ("firstName", "Ana"), ("lastName", "Novak"), ("street", "Main St 1"),
            ("postalCode", "10115"), ("city", " "), ("country", "DE"));

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var html = await response.Content.ReadAsStringAsync();
        Assert.Contains("Please correct the shipping address.", html);
        Assert.Contains("<div class=\"field-error\">City is required</div>", html);
        Assert.Contains("aria-label=\"Cart Line HP-15\"", await (await client.GetAsync("/cart")).Content.ReadAsStringAsync());
    }

    [Fact]
    public async Task QuickOrderCountryComesFromTheProfileOrFallsBackToUs()
    {
        var client = Browser(role: "CUSTOMER");
        await SaveProfileAsync(client, country: "GB");

        var withProfile = await (await client.GetAsync("/new-order")).Content.ReadAsStringAsync();
        var withoutProfile = await (await Browser(role: "CUSTOMER").GetAsync("/new-order")).Content.ReadAsStringAsync();

        Assert.Contains("aria-label=\"Country\" value=\"GB\"", withProfile);
        Assert.Contains("aria-label=\"Country\" value=\"US\"", withoutProfile);
    }

    [Fact]
    public async Task AccountAreaPagesHaveTheAccountSidebar()
    {
        var html = await (await Browser(role: "CUSTOMER").GetAsync("/account")).Content.ReadAsStringAsync();

        Assert.Contains("<nav class=\"account-nav mb-4\" aria-label=\"My account\">", html);
        Assert.Contains("aria-label=\"Profile Country\"", html);
        Assert.Contains("list-group-item list-group-item-action active\" href=\"/account\"", html);
    }

    [Fact]
    public async Task AdminOrderHistoryIsAllOrdersWithACustomerFilter()
    {
        var admin = await (await Browser(role: "ADMIN").GetAsync("/order-history")).Content.ReadAsStringAsync();
        var customer = await (await Browser(role: "CUSTOMER").GetAsync("/order-history")).Content.ReadAsStringAsync();

        Assert.Contains("<h2>All orders</h2>", admin);
        Assert.Contains("aria-label=\"Customer Filter\"", admin);
        Assert.Contains("<h2>My orders</h2>", customer);
        Assert.DoesNotContain("aria-label=\"Customer Filter\"", customer);
    }

    [Theory]
    [InlineData("/cart", "/cart")]
    [InlineData("/?category=Laptops", "/?category=Laptops")]
    [InlineData("//evil.example", "/")]
    [InlineData("https://evil.example", "/")]
    public async Task SignInReturnsToTheRequestedSameSitePage(string returnUrl, string expected)
    {
        var response = await Browser(role: "CUSTOMER").GetAsync("/sign-in?returnUrl=" + Uri.EscapeDataString(returnUrl));

        Assert.Equal(HttpStatusCode.Redirect, response.StatusCode);
        Assert.Equal(expected, response.Headers.Location!.ToString());
    }

    /// <summary>Opens <paramref name="page"/> for its antiforgery token, then posts the form to <paramref name="action"/>.</summary>
    private static async Task<HttpResponseMessage> PostFormAsync(
        HttpClient client, string page, string action, params (string Name, string Value)[] fields)
    {
        var html = await (await client.GetAsync(page)).Content.ReadAsStringAsync();
        var token = AntiforgeryToken().Match(html);
        Assert.True(token.Success, "No antiforgery token on " + page);
        var form = fields.Select(field => new KeyValuePair<string, string>(field.Name, field.Value))
            .Append(new("__RequestVerificationToken", WebUtility.HtmlDecode(token.Groups[1].Value)));
        return await client.PostAsync(action, new FormUrlEncodedContent(form));
    }

    /// <summary>Saves the signed-in customer's profile through the profile API.</summary>
    private static async Task SaveProfileAsync(HttpClient client, string country = "DE")
    {
        var response = await client.PutAsJsonAsync("/api/account/profile", new UpdateProfileRequest
        {
            FirstName = "Ana",
            LastName = "Novak",
            Street = "Main St 1",
            PostalCode = "10115",
            City = "Berlin",
            Country = country,
        });
        response.EnsureSuccessStatusCode();
    }

    private static int Count(string text, string part) => text.Split(part).Length - 1;

    [GeneratedRegex("name=\"__RequestVerificationToken\" type=\"hidden\" value=\"([^\"]+)\"")]
    private static partial Regex AntiforgeryToken();

    [GeneratedRegex("href=\"/order-details\\?orderNumber=([^\"]+)\"")]
    private static partial Regex OrderLink();
}
