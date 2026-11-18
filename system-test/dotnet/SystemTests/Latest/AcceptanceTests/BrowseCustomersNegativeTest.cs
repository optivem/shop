using static SystemTests.Commons.Constants.ErrorMessages;
using SystemTests.Latest.AcceptanceTests.Base;
using Dsl.Core.UseCase;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

// API only: the UI has no customer list of its own (see BrowseCustomersPositiveTest).
public class BrowseCustomersNegativeTest : BaseAcceptanceTest
{
    [Theory]
    [ChannelData(ChannelType.API)]
    [Trait("tag", "UC-011.5")]
    public async Task CustomerShouldNotBeAbleToBrowseCustomers(Channel channel)
    {
        await Scenario(channel)
            .Given().LoggedInAsCustomer()
            .When().BrowseCustomers()
            .Then().ShouldFail()
            .ErrorMessage(PermissionDenied);
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    [Trait("tag", "UC-011.6")]
    public async Task AnonymousShouldNotBeAbleToBrowseCustomers(Channel channel)
    {
        await Scenario(channel)
            .Given().NotLoggedIn()
            .When().BrowseCustomers()
            .Then().ShouldFail()
            .ErrorMessage(AuthenticationRequired);
    }
}
