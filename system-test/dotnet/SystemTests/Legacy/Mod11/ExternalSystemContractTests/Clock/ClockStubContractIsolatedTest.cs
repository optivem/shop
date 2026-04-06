using Dsl.Port;
using SystemTests.Legacy.Mod11.ExternalSystemContractTests.Base;

namespace SystemTests.Legacy.Mod11.ExternalSystemContractTests.Clock;

[Collection("Isolated")]
[Trait("Category", "isolated")]
public class ClockStubContractIsolatedTest : BaseExternalSystemContractTest
{
    protected override ExternalSystemMode? FixedExternalSystemMode => ExternalSystemMode.Stub;

    [Fact]
    public async Task ShouldBeAbleToGetConfiguredTime()
    {
        (await Scenario()
            .Given().Clock().WithTime("2024-01-02T09:00:00Z")
            .Then().Clock())
            .HasTime("2024-01-02T09:00:00Z");
    }
}
