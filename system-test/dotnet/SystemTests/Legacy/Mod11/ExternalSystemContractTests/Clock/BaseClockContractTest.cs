using SystemTests.Legacy.Mod11.ExternalSystemContractTests.Base;

namespace SystemTests.Legacy.Mod11.ExternalSystemContractTests.Clock;

public abstract class BaseClockContractTest : BaseExternalSystemContractTest
{
    [Fact]
    public async Task ShouldBeAbleToGetTime()
    {
        (await Scenario()
            .Given()
            .Then().Clock())
            .HasTime();
    }
}










