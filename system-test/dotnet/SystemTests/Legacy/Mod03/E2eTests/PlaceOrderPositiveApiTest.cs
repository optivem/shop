using System.Net;
using System.Text;
using System.Text.Json;
using SystemTests.Commons.Constants;
using SystemTests.Legacy.Mod03.E2eTests.Base;
using Shouldly;
using Xunit;

namespace SystemTests.Legacy.Mod03.E2eTests;

public class PlaceOrderPositiveApiTest : BaseE2eTest
{
    protected override Task SetMyShopRawAsync()
    {
        SetUpMyShopHttpClient();
        return Task.CompletedTask;
    }

    [Fact]
    public async Task ShouldPlaceOrderForValidInput()
    {
        var sku = CreateUniqueSku(Defaults.SKU);
        var createProductJson = $$"""{"id":"{{sku}}","title":"Test Product","description":"Test Description","category":"Test Category","brand":"Test Brand","price":"20.00"}""";

        var createProductUri = new Uri(_configuration.ErpBaseUrl + "/api/products");
        var createProductContent = new StringContent(createProductJson, Encoding.UTF8, "application/json");
        var createProductResponse = await _erpHttpClient!.PostAsync(createProductUri, createProductContent);
        ((int)createProductResponse.StatusCode).ShouldBe(201);

        var placeOrderJson = $$"""{"sku":"{{sku}}","quantity":"5","country":"US"}""";

        var placeOrderUri = new Uri(_configuration.MyShopApiBaseUrl + "/api/orders");
        var placeOrderContent = new StringContent(placeOrderJson, Encoding.UTF8, "application/json");
        var placeOrderRequest = await WithCustomerAuthAsync(new HttpRequestMessage(HttpMethod.Post, placeOrderUri) { Content = placeOrderContent });
        var placeOrderResponse = await _shopApiHttpClient!.SendAsync(placeOrderRequest);
        ((int)placeOrderResponse.StatusCode).ShouldBe(201);

        var placeOrderBody = await placeOrderResponse.Content.ReadAsStringAsync();
        using var placeOrderDoc = JsonDocument.Parse(placeOrderBody);
        var orderNumber = placeOrderDoc.RootElement.GetProperty("orderNumber").GetString()!;
        orderNumber.ShouldStartWith("ORD-");

        var viewOrderUri = new Uri(_configuration.MyShopApiBaseUrl + $"/api/orders/{orderNumber}");
        var viewOrderRequest = await WithCustomerAuthAsync(new HttpRequestMessage(HttpMethod.Get, viewOrderUri));
        var viewOrderResponse = await _shopApiHttpClient!.SendAsync(viewOrderRequest);
        ((int)viewOrderResponse.StatusCode).ShouldBe((int)HttpStatusCode.OK);

        var viewOrderBody = await viewOrderResponse.Content.ReadAsStringAsync();
        using var viewOrderDoc = JsonDocument.Parse(viewOrderBody);
        var order = viewOrderDoc.RootElement;
        order.GetProperty("orderNumber").GetString().ShouldBe(orderNumber);
        order.GetProperty("sku").GetString().ShouldBe(sku);
        order.GetProperty("quantity").GetInt32().ShouldBe(5);
        order.GetProperty("unitPrice").GetDecimal().ShouldBe(20.00m);
        order.GetProperty("basePrice").GetDecimal().ShouldBe(100.00m);
        order.GetProperty("totalPrice").GetDecimal().ShouldBeGreaterThan(0);
        order.GetProperty("status").GetString().ShouldBe("PLACED");
    }
}
