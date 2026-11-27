using SystemTests.Latest.Base;
using Xunit;

namespace SystemTests.Latest.SmokeTests.System;

public class IdentityProviderSmokeTest : BaseScenarioDslTest
{
    [Fact]
    public async Task ShouldBeAbleToGoToIdentityProvider()
    {
        await Scenario().Assume().IdentityProvider().ShouldBeRunning();
    }
}
