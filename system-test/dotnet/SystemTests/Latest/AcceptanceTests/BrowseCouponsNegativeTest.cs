using SystemTests.Latest.AcceptanceTests.Base;
using Dsl.Core.UseCase;
using Optivem.Testing;
using static SystemTests.Commons.Constants.ErrorMessages;

namespace SystemTests.Latest.AcceptanceTests;

public class BrowseCouponsNegativeTest : BaseAcceptanceTest
{
    [RequiresKeycloakTheory]
    [ChannelData(ChannelType.API)]
    public async Task CustomerShouldNotBeAbleToBrowseCoupons(Channel channel)
    {
        await Scenario(channel)
            .Given().LoggedInAsCustomer()
            .When().BrowseCoupons()
            .Then().ShouldFail()
            .ErrorMessage(PermissionDenied);
    }
}
