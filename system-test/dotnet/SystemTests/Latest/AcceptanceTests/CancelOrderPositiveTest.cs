using SystemTests.Latest.AcceptanceTests.Base;
using Dsl.Core.UseCase;
using Driver.Port.Dtos;
using Common.Domain;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

public class CancelOrderPositiveTest : BaseAcceptanceTest
{
    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task ShouldHaveCancelledStatusWhenCancelled(Channel channel)
    {
        await Scenario(channel)
            .Given().Order()
            .When().CancelOrder()
            .Then().ShouldSucceed()
            .And().Order()
            .HasStatus(OrderStatus.Cancelled);
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task AdminShouldBeAbleToCancelCustomerOrder(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().PlacedByCustomer()
            .And().LoggedInAsAdmin()
            .When().CancelOrder()
            .Then().ShouldSucceed()
            .And().Order()
            .HasStatus(OrderStatus.Cancelled);
    }
}
