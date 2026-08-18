using Common;
using Driver.Adapter.External.Erp.Client.Dtos;
using Driver.Adapter.Ui.Client.Pages;
using Driver.Port.Dtos;
using Common.Domain;
using SystemTests.Commons.Constants;
using SystemTests.Legacy.Mod04.E2eTests.Base;
using Shouldly;
using Xunit;

namespace SystemTests.Legacy.Mod04.E2eTests;

public class PlaceOrderPositiveUiTest : BaseE2eTest
{
    protected override Task SetMyShopClientAsync()
    {
        return SetUpMyShopUiClientAsync();
    }

    [Fact]
    public async Task ShouldPlaceOrderForValidInput()
    {
        // GivenStage
        var sku = CreateUniqueSku(Defaults.SKU);
        (await _erpClient!.CreateProductAsync(new ExtCreateProductRequest { Id = sku, Title = "Test Product", Description = "Test Description", Category = "Test Category", Brand = "Test Brand", Price = "20.00" })).ShouldBeSuccess();

        // WhenStage
        var homePage = await _shopUiClient!.OpenHomePageAsync();
        var newOrderPage = await homePage.ClickNewOrderAsync();
        await newOrderPage.InputSkuAsync(sku);
        await newOrderPage.InputQuantityAsync("5");
        await newOrderPage.InputCountryAsync(Defaults.COUNTRY);
        await newOrderPage.ClickPlaceOrderAsync();

        var placeOrderResult = await newOrderPage.GetResultAsync();
        placeOrderResult.ShouldBeSuccess();

        var orderNumber = NewOrderPage.GetOrderNumber(placeOrderResult.Value!);
        orderNumber.ShouldStartWith("ORD-");

        // ThenStage
        var orderHistoryPage = await (await _shopUiClient.OpenHomePageAsync()).ClickOrderHistoryAsync();
        await orderHistoryPage.InputOrderNumberAsync(orderNumber);
        await orderHistoryPage.ClickSearchAsync();
        (await orderHistoryPage.WaitForOrderRowAsync(orderNumber)).ShouldBeTrue();

        var orderDetailsPage = await orderHistoryPage.ClickViewOrderDetailsAsync(orderNumber);
        (await orderDetailsPage.GetOrderNumberAsync()).ShouldBe(orderNumber);
        (await orderDetailsPage.GetSkuAsync()).ShouldBe(sku);
        (await orderDetailsPage.GetQuantityAsync()).ShouldBe(5);
        (await orderDetailsPage.GetUnitPriceAsync()).ShouldBe(20.00m);
        (await orderDetailsPage.GetTotalPriceAsync()).ShouldBeGreaterThan(0);
        (await orderDetailsPage.GetStatusAsync()).ShouldBe(OrderStatus.Placed);
    }
}
