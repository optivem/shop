using Common;
using Dsl.Core.UseCase;
using Driver.Port.Dtos;
using Common.Domain;
using Driver.Port.External.Erp.Dtos;
using SystemTests.Commons.Constants;
using SystemTests.Legacy.Mod06.E2eTests.Base;
using Optivem.Testing;
using Shouldly;
using Xunit;

namespace SystemTests.Legacy.Mod06.E2eTests;

public class PlaceOrderPositiveTest : BaseE2eTest
{
    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task ShouldPlaceOrderForValidInput(Channel channel)
    {
        await SetChannelAsync(channel);

        // GivenStage
        var sku = CreateUniqueSku(Defaults.SKU);
        (await _erpDriver!.ReturnsProductAsync(new ReturnsProductRequest
        {
            Sku = sku,
            Price = "20.00"
        }))
            .ShouldBeSuccess();

        // WhenStage
        var placeOrderRequest = new PlaceOrderRequest
        {
            Sku = sku,
            Quantity = "5",
            Country = Defaults.COUNTRY,
        };
        var placeOrderResult = await _shopDriver!.PlaceOrderAsync(placeOrderRequest);
        placeOrderResult.ShouldBeSuccess();

        var orderNumber = placeOrderResult.Value!.OrderNumber;
        orderNumber.ShouldStartWith("ORD-");

        // ThenStage
        var viewOrderResult = await _shopDriver.ViewOrderAsync(new ViewOrderRequest { OrderNumber = orderNumber });
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
