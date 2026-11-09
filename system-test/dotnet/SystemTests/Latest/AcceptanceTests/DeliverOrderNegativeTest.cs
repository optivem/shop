using SystemTests.Latest.AcceptanceTests.Base;
using Dsl.Core.UseCase;
using Common.Domain;
using Optivem.Testing;
using static SystemTests.Commons.Constants.ErrorMessages;

namespace SystemTests.Latest.AcceptanceTests;

public class DeliverOrderNegativeTest : BaseAcceptanceTest
{
    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-012.4")]
    public async Task CustomerShouldNotBeAbleToDeliverOrder(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().PlacedByCustomer()
            .And().LoggedInAsCustomer()
            .When().DeliverOrder()
            .Then().ShouldFail()
            .ErrorMessage(PermissionDenied)
            .And().Order()
            .HasStatus(OrderStatus.Placed);
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    [Trait("tag", "UC-012.5")]
    public async Task ShouldNotDeliverNonExistentOrder(Channel channel)
    {
        await Scenario(channel)
            .Given().LoggedInAsAdmin()
            .When().DeliverOrder().WithOrderNumber("NON-EXISTENT-ORDER-99999")
            .Then().ShouldFail()
            .ErrorMessage("Order NON-EXISTENT-ORDER-99999 does not exist.");
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    [ChannelInlineData(OrderStatus.Cancelled)]
    [ChannelInlineData(OrderStatus.Delivered)]
    [Trait("tag", "UC-012.6")]
    public async Task ShouldNotDeliverOrderThatIsNotPlaced(Channel channel, OrderStatus status)
    {
        await Scenario(channel)
            .Given().Order().PlacedByCustomer().WithStatus(status)
            .And().LoggedInAsAdmin()
            .When().DeliverOrder()
            .Then().ShouldFail()
            .ErrorMessage("Order cannot be delivered in its current status")
            .And().Order()
            .HasStatus(status);
    }
}
