using SystemTests.Latest.AcceptanceTests.Base;
using Dsl.Core.UseCase;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

public class ViewOrderPositiveTest : BaseAcceptanceTest
{
    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task ShouldBeAbleToViewOrder(Channel channel)
    {
        await Scenario(channel)
            .Given().Order()
            .When().ViewOrder()
            .Then().ShouldSucceed();
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task CustomerShouldBeAbleToViewOwnOrder(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().PlacedByCustomer()
            .And().LoggedInAsCustomer()
            .When().ViewOrder()
            .Then().ShouldSucceed();
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task SameCustomerAliasShouldResolveToSameCustomer(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().PlacedByCustomer("B")
            .And().LoggedInAsCustomer("B")
            .When().ViewOrder()
            .Then().ShouldSucceed();
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task AdminShouldBeAbleToViewCustomerOrder(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().PlacedByCustomer()
            .And().LoggedInAsAdmin()
            .When().ViewOrder()
            .Then().ShouldSucceed();
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task AdminShouldBeAbleToViewAnotherCustomersOrder(Channel channel)
    {
        await Scenario(channel)
            .Given().Order().PlacedByCustomer("B")
            .And().LoggedInAsAdmin()
            .When().ViewOrder()
            .Then().ShouldSucceed();
    }
}
