using SystemTests.Legacy.Mod10.AcceptanceTests.Base;
using Dsl.Core.Shop;
using Optivem.Testing;
using Xunit;

namespace SystemTests.Legacy.Mod10.AcceptanceTests;

[Collection("Isolated")]
[Trait("Category", "isolated")]
public class PlaceOrderNegativeIsolatedTest : BaseAcceptanceTest
{
    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task ShouldRejectOrderPlacedAtYearEnd(Channel channel)
    {
        await Scenario(channel)
            .Given().Clock()
                .WithTime("2026-12-31T23:59:30Z")
            .When().PlaceOrder()
            .Then().ShouldFail()
                .ErrorMessage("Orders cannot be placed between 23:59 and 00:00 on December 31st");
    }
}
