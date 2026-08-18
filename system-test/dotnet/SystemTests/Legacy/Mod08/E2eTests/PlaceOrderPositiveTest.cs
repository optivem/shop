using Dsl.Core.UseCase;
using Driver.Port.Dtos;
using Common.Domain;
using SystemTests.Legacy.Mod08.E2eTests.Base;
using Optivem.Testing;
using Xunit;

namespace SystemTests.Legacy.Mod08.E2eTests;

public class PlaceOrderPositiveTest : BaseE2eTest
{
    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task ShouldPlaceOrderForValidInput(Channel channel)
    {
        await Scenario(channel)
            .Given().Product()
                .WithUnitPrice(20.00m)
            .When().PlaceOrder()
                .WithQuantity(5)
            .Then().ShouldSucceed()
            .And().Order()
                    .HasOrderNumberPrefix("ORD-")
                    .HasQuantity(5)
                    .HasUnitPrice(20.00m)
                    .HasStatus(OrderStatus.Placed)
                    .HasTotalPriceGreaterThanZero();
    }
}
