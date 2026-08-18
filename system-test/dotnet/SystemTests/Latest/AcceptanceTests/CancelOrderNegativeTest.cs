using SystemTests.Latest.AcceptanceTests.Base;
using Dsl.Core.UseCase;
using Driver.Port.Dtos;
using Common.Domain;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

public class CancelOrderNegativeTest : BaseAcceptanceTest
{
    [Theory]
    [ChannelData(ChannelType.API)]
    [ChannelInlineData("NON-EXISTENT-ORDER-99999", "Order NON-EXISTENT-ORDER-99999 does not exist.")]
    [ChannelInlineData("NON-EXISTENT-ORDER-88888", "Order NON-EXISTENT-ORDER-88888 does not exist.")]
    [ChannelInlineData("NON-EXISTENT-ORDER-77777", "Order NON-EXISTENT-ORDER-77777 does not exist.")]
    public async Task ShouldNotCancelNonExistentOrder(Channel channel, string orderNumber, string expectedErrorMessage)
    {
        await Scenario(channel)
            .When().CancelOrder().WithOrderNumber(orderNumber)
            .Then().ShouldFail()
            .ErrorMessage(expectedErrorMessage);
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    public async Task ShouldNotCancelAlreadyCancelledOrder(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().WithStatus(OrderStatus.Cancelled)
            .When().CancelOrder()
            .Then().ShouldFail()
            .ErrorMessage("Order has already been cancelled");
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    public async Task CannotCancelNonExistentOrder(Channel channel)
    {
        await Scenario(channel)
            .When().CancelOrder().WithOrderNumber("non-existent-order-12345")
            .Then().ShouldFail()
            .ErrorMessage("Order non-existent-order-12345 does not exist.");
    }
}
