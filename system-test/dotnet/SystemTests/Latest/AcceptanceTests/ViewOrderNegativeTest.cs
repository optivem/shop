using SystemTests.Latest.AcceptanceTests.Base;
using Dsl.Core.Shop;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

public class ViewOrderNegativeTest : BaseAcceptanceTest
{
    public static IEnumerable<object[]> NonExistentOrderValues()
    {
        yield return new object[] { "NON-EXISTENT-ORDER-99999", "Order NON-EXISTENT-ORDER-99999 does not exist." };
        yield return new object[] { "NON-EXISTENT-ORDER-88888", "Order NON-EXISTENT-ORDER-88888 does not exist." };
        yield return new object[] { "NON-EXISTENT-ORDER-77777", "Order NON-EXISTENT-ORDER-77777 does not exist." };
    }

    [Theory]
    [ChannelData(ChannelType.API, AlsoForFirstRow = new[] { ChannelType.UI })]
    [ChannelMemberData(nameof(NonExistentOrderValues))]
    public async Task ShouldNotBeAbleToViewNonExistentOrder(Channel channel, string orderNumber, string expectedErrorMessage)
    {
        await Scenario(channel)
            .When().ViewOrder().WithOrderNumber(orderNumber)
            .Then().ShouldFail()
            .ErrorMessage(expectedErrorMessage);
    }
}
