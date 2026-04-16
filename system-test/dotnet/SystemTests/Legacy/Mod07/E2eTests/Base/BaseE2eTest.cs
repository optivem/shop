using Dsl.Port;
using Dsl.Core.Shared;
using SystemTests.Legacy.Mod07.Base;

namespace SystemTests.Legacy.Mod07.E2eTests.Base;

public abstract class BaseE2eTest : BaseUseCaseDslTest
{
    protected override ExternalSystemMode? GetFixedExternalSystemMode()
    {
        return ExternalSystemMode.Real;
    }
}












