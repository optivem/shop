using System.Net;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using Xunit;

namespace MyCompany.MyShop.Monolith.Tests;

/// <summary>
/// The HTTP half of the profile and of the shipping address on orders: the profile is the caller's own
/// (names and email from the token until one is saved), and invalid profile and address fields come back
/// as one validation problem listing every failing field under its camelCase JSON path.
/// </summary>
public class ProfileAndShippingAddressTests : IClassFixture<ShopWebApplicationFactory>
{
    private const string ValidationDetail = "The request contains one or more validation errors";
    private const string ValidProfile =
        "{\"firstName\":\"Ana\",\"lastName\":\"Novak\",\"street\":\"Main St 1\",\"postalCode\":\"10115\",\"city\":\"Berlin\",\"country\":\"DE\"}";

    private readonly ShopWebApplicationFactory _factory;

    public ProfileAndShippingAddressTests(ShopWebApplicationFactory factory) => _factory = factory;

    /// <summary>A fresh subject per test, so tests sharing the in-memory database never see each other's profile.</summary>
    private string NewCustomerToken() =>
        _factory.CreateToken("CUSTOMER", subject: $"customer-{Guid.NewGuid()}", extraClaims: new Dictionary<string, string>
        {
            ["email"] = "customer1@example.com",
            ["given_name"] = "Ana",
            ["family_name"] = "Novak",
        });

    private async Task<HttpResponseMessage> Send(HttpMethod method, string path, string? token, string? body = null)
    {
        var request = new HttpRequestMessage(method, path);
        if (token != null)
            request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);
        if (body != null)
            request.Content = new StringContent(body, Encoding.UTF8, "application/json");
        return await _factory.CreateClient(new() { AllowAutoRedirect = false }).SendAsync(request);
    }

    private static async Task<JsonElement> Json(HttpResponseMessage response) =>
        JsonDocument.Parse(await response.Content.ReadAsStringAsync()).RootElement;

    private static Dictionary<string, string> FieldErrors(JsonElement problem) =>
        problem.GetProperty("errors").EnumerateArray()
            .ToDictionary(e => e.GetProperty("field").GetString()!, e => e.GetProperty("message").GetString()!);

    [Fact]
    public async Task ViewProfile_WithoutSavedProfileAnswersWithTheAccountNamesAndEmail()
    {
        var response = await Send(HttpMethod.Get, "/api/account/profile", NewCustomerToken());

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var body = await Json(response);
        Assert.Equal("Ana", body.GetProperty("firstName").GetString());
        Assert.Equal("Novak", body.GetProperty("lastName").GetString());
        Assert.Equal("customer1@example.com", body.GetProperty("email").GetString());
        Assert.Equal(JsonValueKind.Null, body.GetProperty("street").ValueKind);
        Assert.Equal(JsonValueKind.Null, body.GetProperty("country").ValueKind);
    }

    [Fact]
    public async Task ViewProfile_WithoutTokenIsUnauthorized()
    {
        var response = await Send(HttpMethod.Get, "/api/account/profile", null);

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task UpdateProfile_SavesTheProfileAndAnswersWithIt()
    {
        var token = NewCustomerToken();

        var update = await Send(HttpMethod.Put, "/api/account/profile", token,
            "{\"firstName\":\" Ana \",\"lastName\":\"Novak\",\"street\":\"1 High St\",\"postalCode\":\"SW1A 1AA\",\"city\":\"London\",\"country\":\"GB\"}");

        Assert.Equal(HttpStatusCode.OK, update.StatusCode);
        var saved = await Json(update);
        Assert.Equal("Ana", saved.GetProperty("firstName").GetString());
        Assert.Equal("customer1@example.com", saved.GetProperty("email").GetString());

        var viewed = await Json(await Send(HttpMethod.Get, "/api/account/profile", token));
        Assert.Equal("1 High St", viewed.GetProperty("street").GetString());
        Assert.Equal("SW1A 1AA", viewed.GetProperty("postalCode").GetString());
        Assert.Equal("London", viewed.GetProperty("city").GetString());
        Assert.Equal("GB", viewed.GetProperty("country").GetString());
    }

    [Fact]
    public async Task UpdateProfile_ReportsEveryEmptyOrBlankField()
    {
        var response = await Send(HttpMethod.Put, "/api/account/profile", NewCustomerToken(),
            "{\"firstName\":null,\"lastName\":\"\",\"street\":\"   \",\"city\":\" \",\"country\":\"\"}");

        Assert.Equal(HttpStatusCode.UnprocessableEntity, response.StatusCode);
        var problem = await Json(response);
        Assert.Equal(ValidationDetail, problem.GetProperty("detail").GetString());
        Assert.Equal(new Dictionary<string, string>
        {
            ["firstName"] = "First name is required",
            ["lastName"] = "Last name is required",
            ["street"] = "Street is required",
            ["postalCode"] = "Postal code is required",
            ["city"] = "City is required",
            ["country"] = "Country is required",
        }, FieldErrors(problem));
    }

    [Theory]
    [InlineData("FR")]
    [InlineData("de")]
    public async Task UpdateProfile_RejectsAnUnsupportedCountry(string country)
    {
        var response = await Send(HttpMethod.Put, "/api/account/profile", NewCustomerToken(),
            ValidProfile.Replace("\"DE\"", $"\"{country}\""));

        Assert.Equal(HttpStatusCode.UnprocessableEntity, response.StatusCode);
        Assert.Equal(new Dictionary<string, string> { ["country"] = "Country must be US, DE or GB" },
            FieldErrors(await Json(response)));
    }

    [Fact]
    public async Task UpdateProfile_RejectedChangeLeavesTheSavedProfile()
    {
        var token = NewCustomerToken();
        await Send(HttpMethod.Put, "/api/account/profile", token, ValidProfile);

        var rejected = await Send(HttpMethod.Put, "/api/account/profile", token,
            ValidProfile.Replace("\"Main St 1\"", "\"\""));

        Assert.Equal(HttpStatusCode.UnprocessableEntity, rejected.StatusCode);
        var viewed = await Json(await Send(HttpMethod.Get, "/api/account/profile", token));
        Assert.Equal("Main St 1", viewed.GetProperty("street").GetString());
    }

    [Fact]
    public async Task PlaceOrder_ReportsEveryEmptyShippingAddressFieldWithTheOtherFieldErrors()
    {
        var response = await Send(HttpMethod.Post, "/api/orders", _factory.CreateToken("CUSTOMER"),
            "{\"sku\":\"BOOK-123\",\"quantity\":0,\"shippingAddress\":" +
            "{\"firstName\":\"\",\"lastName\":\" \",\"street\":null,\"postalCode\":\"\",\"city\":\"   \",\"country\":\"\"}}");

        Assert.Equal(HttpStatusCode.UnprocessableEntity, response.StatusCode);
        var problem = await Json(response);
        Assert.Equal(ValidationDetail, problem.GetProperty("detail").GetString());
        Assert.Equal(new Dictionary<string, string>
        {
            ["quantity"] = "Quantity must be positive",
            ["shippingAddress.firstName"] = "First name is required",
            ["shippingAddress.lastName"] = "Last name is required",
            ["shippingAddress.street"] = "Street is required",
            ["shippingAddress.postalCode"] = "Postal code is required",
            ["shippingAddress.city"] = "City is required",
            ["shippingAddress.country"] = "Country is required",
        }, FieldErrors(problem));
    }

    [Fact]
    public async Task PlaceOrder_WithoutShippingAddressStillRequiresTheCountry()
    {
        var response = await Send(HttpMethod.Post, "/api/orders", _factory.CreateToken("CUSTOMER"),
            "{\"sku\":\"BOOK-123\",\"quantity\":1,\"country\":\"  \"}");

        Assert.Equal(HttpStatusCode.UnprocessableEntity, response.StatusCode);
        Assert.Equal(new Dictionary<string, string> { ["country"] = "Country must not be empty" },
            FieldErrors(await Json(response)));
    }
}
