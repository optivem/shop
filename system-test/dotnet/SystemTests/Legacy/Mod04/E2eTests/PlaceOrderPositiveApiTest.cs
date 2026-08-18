using Common;
using Driver.Adapter.External.Erp.Client.Dtos;
using Driver.Port.Dtos;
using Common.Domain;
using SystemTests.Commons.Constants;
using SystemTests.Legacy.Mod04.E2eTests.Base;
using Shouldly;
using Xunit;

namespace SystemTests.Legacy.Mod04.E2eTests;

public class PlaceOrderPositiveApiTest : BaseE2eTest
{
    protected override Task SetMyShopClientAsync()
    {
        SetUpMyShopApiClient();
        return Task.CompletedTask;
    }

    [Fact]
    public async Task ShouldPlaceOrderForValidInput()
    {
        // GivenStage
        var sku = CreateUniqueSku(Defaults.SKU);
        (await _erpClient!.CreateProductAsync(new ExtCreateProductRequest { Id = sku, Title = "Test Product", Description = "Test Description", Category = "Test Category", Brand = "Test Brand", Price = "20.00" })).ShouldBeSuccess();

        // WhenStage
        var placeOrderRequest = new PlaceOrderRequest { Sku = sku, Quantity = "5", Country = Defaults.COUNTRY };
        var placeOrderResult = await _shopApiClient!.Orders().PlaceOrderAsync(placeOrderRequest);
        placeOrderResult.ShouldBeSuccess();

        var orderNumber = placeOrderResult.Value!.OrderNumber;
        orderNumber.ShouldStartWith("ORD-");

        // ThenStage
        var viewOrderResult = await _shopApiClient.Orders().ViewOrderAsync(orderNumber);
        viewOrderResult.ShouldBeSuccess();

        var order = viewOrderResult.Value!;
        order.OrderNumber.ShouldBe(orderNumber);
        order.Sku.ShouldBe(sku);
        order.Quantity.ShouldBe(5);
        order.UnitPrice.ShouldBe(20.00m);
        order.TotalPrice.ShouldBeGreaterThan(0);
        order.Status.ShouldBe(OrderStatus.Placed);
    }
}
