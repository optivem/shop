using SystemTests.Latest.AcceptanceTests.Base;
using Dsl.Core.UseCase;
using Common.Domain;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

public class DeliverOrderPositiveTest : BaseAcceptanceTest
{
    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-012.1")]
    public async Task ShouldHaveDeliveredStatusWhenDelivered(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().PlacedByCustomer()
            .And().LoggedInAsAdmin()
            .When().DeliverOrder()
            .Then().ShouldSucceed()
            .And().Order()
            .HasStatus(OrderStatus.Delivered);
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-012.3")]
    public async Task AdminShouldBeAbleToDeliverAnotherCustomersOrder(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().PlacedByCustomer("B")
            .And().LoggedInAsAdmin()
            .When().DeliverOrder()
            .Then().ShouldSucceed()
            .And().Order()
            .HasStatus(OrderStatus.Delivered);
    }

    [Theory]
    [ChannelData(ChannelType.UI)]
    [Trait("tag", "UC-012.2")]
    public async Task ShouldOfferToDeliverAPlacedOrder(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().PlacedByCustomer()
            .And().LoggedInAsAdmin()
            .When().ViewOrder()
            .Then().ShouldSucceed()
            .And().Order()
            .OffersDeliverOrder();
    }
}
