using Dsl.Core.UseCase;
using Driver.Port.Dtos;
using Common.Domain;
using SystemTests.Commons.Constants;
using SystemTests.Legacy.Mod07.E2eTests.Base;
using Optivem.Testing;
using Xunit;

namespace SystemTests.Legacy.Mod07.E2eTests;

public class PlaceOrderPositiveTest : BaseE2eTest
{
    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task ShouldPlaceOrderForValidInput(Channel channel)
    {
        (await _app.Erp()
            .ReturnsProduct()
            .Sku(Defaults.SKU)
            .UnitPrice(20.00m)
            .Execute())
            .ShouldSucceed();

        var shop = await _app.MyShop(channel);
        (await shop.PlaceOrder()
            .OrderNumber(Defaults.ORDER_NUMBER)
            .Sku(Defaults.SKU)
            .Quantity(5)
            .Country(Defaults.COUNTRY)
            .Execute())
            .ShouldSucceed()
            .OrderNumber(Defaults.ORDER_NUMBER)
            .OrderNumberStartsWith("ORD-");

        (await shop.ViewOrder()
            .OrderNumber(Defaults.ORDER_NUMBER)
            .Execute())
            .ShouldSucceed()
            .OrderNumber(Defaults.ORDER_NUMBER)
            .Sku(Defaults.SKU)
            .Quantity(5)
            .UnitPrice(20.00m)
            .Status(OrderStatus.Placed)
            .TotalPriceGreaterThanZero();
    }
}
