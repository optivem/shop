using SystemTests.Latest.ExternalSystemContractTests.Base;

namespace SystemTests.Latest.ExternalSystemContractTests.Clock;

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










