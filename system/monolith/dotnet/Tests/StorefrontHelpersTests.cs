using Microsoft.AspNetCore.Http;
using MyCompany.MyShop.Monolith.Core.Dtos;
using MyCompany.MyShop.Monolith.Pages.Storefront;
using Xunit;

namespace MyCompany.MyShop.Monolith.Tests;

/// <summary>The storefront's display and rule helpers: money, quantity, the cart, catalog queries, addresses and return URLs.</summary>
public class StorefrontHelpersTests
{
    private static readonly List<ViewProductDetailsResponse> Products =
    [
        Product("HP-15", "HP Laptop 15", "HP", "Laptops", 699.99m),
        Product("DELL-XPS", "dell XPS 13", "Dell", "Laptops", 1299.99m),
        Product("LOGI-MX3S", "Logitech MX Master 3S", "Logitech", "Accessories", 99.99m),
    ];

    [Theory]
    [InlineData("1299.99", "$1,299.99")]
    [InlineData("0", "$0.00")]
    [InlineData("12.345", "$12.35")]
    public void Money_FormatsWithThousandsSeparatorsAndTwoDecimals(string amount, string expected) =>
        Assert.Equal(expected, Money.Format(decimal.Parse(amount, System.Globalization.CultureInfo.InvariantCulture)));

    [Theory]
    [InlineData("1", 1)]
    [InlineData(" 99 ", 99)]
    [InlineData("0", null)]
    [InlineData("-1", null)]
    [InlineData("1.5", null)]
    [InlineData("abc", null)]
    [InlineData("100", null)]
    [InlineData("", null)]
    public void Quantity_AcceptsOnlyWholeNumbersFrom1To99(string typed, int? expected) =>
        Assert.Equal(expected, Quantity.Parse(typed));

    [Fact]
    public void Cart_KeepsFirstAddedOrderAndCapsEachLine()
    {
        var cart = Cart.Empty();
        cart.Add("HP-15", 1);
        cart.Add("DELL-XPS", 2);
        cart.Add("HP-15", 98);
        cart.Add("HP-15", 5);

        Assert.Equal([new CartLine("HP-15", 99), new CartLine("DELL-XPS", 2)], cart.Lines);
        Assert.Equal(101, cart.TotalQuantity);

        cart.SetQuantity("DELL-XPS", 0);
        cart.SetQuantity("NOT-IN-CART", 3);
        cart.RemoveAll(["HP-15"]);
        Assert.Equal([new CartLine("DELL-XPS", 1)], cart.Lines);
    }

    [Fact]
    public void Cart_SurvivesTheSession()
    {
        var session = new FakeSession();
        var cart = Cart.Load(session);
        Assert.True(cart.IsEmpty);
        Assert.Empty(session.Keys);

        cart.Add("HP-15", 2);
        cart.Save(session);

        Assert.Equal([new CartLine("HP-15", 2)], Cart.Load(session).Lines);
    }

    [Fact]
    public void CatalogQuery_FiltersByTermCategoryAndBrandAndSorts()
    {
        var query = CatalogQuery.Parse("", "Laptops", ["Dell", "", "HP"], "name");

        Assert.Equal(["DELL-XPS", "HP-15"], query.Apply(Products).Select(p => p.Sku));
        Assert.Equal("/?category=Laptops&brand=Dell&brand=HP&sort=name", query.Path());

        var searched = CatalogQuery.Parse("  LAPTOP 15 ", null, null, null);
        Assert.Equal(["HP-15"], searched.Apply(Products).Select(p => p.Sku));
        Assert.Equal("/?q=LAPTOP+15", searched.Path());
    }

    [Fact]
    public void CatalogQuery_DefaultsToFeaturedInErpOrderAtTheShortAddress()
    {
        var query = CatalogQuery.Parse(null, null, null, "no-such-sort");

        Assert.Equal(CatalogQuery.Featured, query.Sort);
        Assert.Equal(["HP-15", "DELL-XPS", "LOGI-MX3S"], query.Apply(Products).Select(p => p.Sku));
        Assert.Equal("/", query.Path());
        Assert.Equal(["Laptops", "Accessories"], CatalogQuery.CategoriesOf(Products));
        Assert.Equal(["HP", "Dell", "Logitech"], CatalogQuery.BrandsOf(Products));
    }

    [Fact]
    public void CatalogQuery_SortsByPrice()
    {
        Assert.Equal(["LOGI-MX3S", "HP-15", "DELL-XPS"],
            CatalogQuery.Parse(null, null, null, "price-asc").Apply(Products).Select(p => p.Sku));
        Assert.Equal(["DELL-XPS", "HP-15", "LOGI-MX3S"],
            CatalogQuery.Parse(null, null, null, "price-desc").Apply(Products).Select(p => p.Sku));
    }

    [Theory]
    [InlineData("?q=&category=Laptops", true)]
    [InlineData("?sort=featured", true)]
    [InlineData("?category=Laptops&sort=name", false)]
    [InlineData("", false)]
    public void CatalogQuery_SpotsTheFilterFormsDefaults(string queryString, bool expected) =>
        Assert.Equal(expected, CatalogQuery.HasDefaultParams(new QueryCollection(
            Microsoft.AspNetCore.WebUtilities.QueryHelpers.ParseQuery(queryString))));

    [Fact]
    public void ShippingAddresses_RequiresEveryFieldAndASupportedCountry()
    {
        var blank = ShippingAddresses.Validate(new ShippingAddress { FirstName = " " });
        Assert.Equal(["firstName", "lastName", "street", "postalCode", "city", "country"], blank.Select(e => e.Key));
        Assert.Equal("City is required", blank.Single(e => e.Key == "city").Value);

        var france = ShippingAddresses.Validate(Address("FR"));
        Assert.Equal([new KeyValuePair<string, string>("country", "Country must be US, DE or GB")], france);

        Assert.Empty(ShippingAddresses.Validate(Address("DE")));
    }

    [Fact]
    public void ShippingAddresses_SavedAddressIsOnlyACompleteOne()
    {
        Assert.Null(ShippingAddresses.SavedAddressOf(null));
        Assert.Null(ShippingAddresses.SavedAddressOf(new ViewProfileResponse { FirstName = "Ana" }));

        var saved = ShippingAddresses.SavedAddressOf(new ViewProfileResponse
        {
            FirstName = "Ana",
            LastName = "Novak",
            Street = "Main St 1",
            PostalCode = "10115",
            City = "Berlin",
            Country = "DE",
        });
        Assert.Equal("Ana Novak, Main St 1, 10115 Berlin, DE", ShippingAddresses.Format(saved!));
    }

    [Theory]
    [InlineData("US", "0%")]
    [InlineData("DE", "19%")]
    [InlineData(" GB ", "20%")]
    [InlineData("FR", "-")]
    [InlineData(null, "-")]
    public void ShippingAddresses_EstimatesTheTaxRateOfTheCountry(string? country, string expected) =>
        Assert.Equal(expected, ShippingAddresses.EstimatedTaxRate(country));

    [Theory]
    [InlineData("/cart", "/cart")]
    [InlineData("/?q=a&b=c", "/?q=a&b=c")]
    [InlineData("//evil.example", "/")]
    [InlineData("/\\evil.example", "/")]
    [InlineData("https://evil.example", "/")]
    [InlineData(null, "/")]
    public void ReturnUrls_AcceptOnlySameSitePaths(string? returnUrl, string expected) =>
        Assert.Equal(expected, ReturnUrls.Safe(returnUrl));

    [Fact]
    public void BrandColour_IsTheSameForTheSameBrand()
    {
        Assert.Equal(BrandColour.Of("Dell"), BrandColour.Of("Dell"));
        Assert.Matches("^#[0-9a-f]{6}$", BrandColour.Of(null));
    }

    private static ShippingAddress Address(string country) => new()
    {
        FirstName = "Ana",
        LastName = "Novak",
        Street = "Main St 1",
        PostalCode = "10115",
        City = "Berlin",
        Country = country,
    };

    private static ViewProductDetailsResponse Product(string sku, string title, string brand, string category, decimal price) =>
        new() { Sku = sku, Title = title, Description = title, Brand = brand, Category = category, Price = price };

    private sealed class FakeSession : ISession
    {
        private readonly Dictionary<string, byte[]> _values = new();

        public bool IsAvailable => true;
        public string Id => "fake";
        public IEnumerable<string> Keys => _values.Keys;
        public void Clear() => _values.Clear();
        public Task CommitAsync(CancellationToken cancellationToken = default) => Task.CompletedTask;
        public Task LoadAsync(CancellationToken cancellationToken = default) => Task.CompletedTask;
        public void Remove(string key) => _values.Remove(key);
        public void Set(string key, byte[] value) => _values[key] = value;
        public bool TryGetValue(string key, out byte[] value) => _values.TryGetValue(key, out value!);
    }
}
