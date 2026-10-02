using System.Text;
using System.Text.Json;
using SystemTests.Commons.Constants;
using SystemTests.Legacy.Mod03.E2eTests.Base;
using Shouldly;
using Xunit;

namespace SystemTests.Legacy.Mod03.E2eTests;

public class PlaceOrderNegativeApiTest : BaseE2eTest
{
    protected override Task SetMyShopRawAsync()
    {
        SetUpMyShopHttpClient();
        return Task.CompletedTask;
    }

    [Fact]
    public async Task ShouldRejectOrderWithNonIntegerQuantity()
    {
        var placeOrderJson = $$"""{"sku":"{{CreateUniqueSku(Defaults.SKU)}}","quantity":"invalid-quantity"}""";

        var uri = new Uri(_configuration.MyShopApiBaseUrl + "/api/orders");
        var content = new StringContent(placeOrderJson, Encoding.UTF8, "application/json");
        var request = await WithCustomerAuthAsync(new HttpRequestMessage(HttpMethod.Post, uri) { Content = content });
        var response = await _shopApiHttpClient!.SendAsync(request);

        ((int)response.StatusCode).ShouldBe(422);
        var body = await response.Content.ReadAsStringAsync();
        using var doc = JsonDocument.Parse(body);
        var root = doc.RootElement;
        root.GetProperty("detail").GetString().ShouldBe("The request contains one or more validation errors");
        var errors = root.GetProperty("errors");
        errors.GetArrayLength().ShouldBeGreaterThan(0);
        var found = false;
        foreach (var err in errors.EnumerateArray())
        {
            if (err.GetProperty("field").GetString() == "quantity" && err.GetProperty("message").GetString() == "Quantity must be an integer")
            {
                found = true;
                break;
            }
        }
        found.ShouldBeTrue();
    }
}
