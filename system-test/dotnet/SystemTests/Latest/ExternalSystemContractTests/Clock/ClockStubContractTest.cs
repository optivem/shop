using Dsl.Port;

namespace SystemTests.Latest.ExternalSystemContractTests.Clock;

public class ClockStubContractTest : BaseClockContractTest
{
    protected override ExternalSystemMode? FixedExternalSystemMode => ExternalSystemMode.Stub;
}












