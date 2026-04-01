using SystemTests.Latest.AcceptanceTests.Base;
using Driver.Port.Shop.Dtos;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

public class PlaceOrderPositiveTest : BaseAcceptanceTest
{
    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task OrderNumberShouldStartWithORD(Channel channel)
    {
        await Scenario(channel)
            .When().PlaceOrder()
            .Then().ShouldSucceed()
            .And().Order()
            .HasOrderNumberPrefix("ORD-");
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task OrderStatusShouldBePlacedAfterPlacingOrder(Channel channel)
    {
        await Scenario(channel)
            .When().PlaceOrder()
            .Then().ShouldSucceed()
            .And().Order()
            .HasStatus(OrderStatus.Placed);
    }
}
