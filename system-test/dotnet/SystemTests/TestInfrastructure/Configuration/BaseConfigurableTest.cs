using Dsl.Port;
using Dsl.Core.Shared;
using Dsl.Core;

namespace SystemTests.TestInfrastructure.Configuration;

public abstract class BaseConfigurableTest
{
    // xUnit interleaves async test lifecycles even within the same class,
    // causing tests that configure shared WireMock stubs to overwrite each
    // other's stubs mid-flight. This semaphore forces truly sequential
    // execution across all system tests. See optivem/starter#20.
    protected static readonly SemaphoreSlim TestLock = new(1, 1);

    protected virtual Environment? GetFixedEnvironment()
    {
        return null;
    }

    protected virtual ExternalSystemMode? GetFixedExternalSystemMode()
    {
        return null;
    }

    protected Dsl.Core.Configuration LoadConfiguration()
    {
        var fixedEnvironment = GetFixedEnvironment();
        var fixedExternalSystemMode = GetFixedExternalSystemMode();

        var environment = PropertyLoader.GetEnvironment(fixedEnvironment);
        var externalSystemMode = PropertyLoader.GetExternalSystemMode(fixedExternalSystemMode);

        return SystemConfigurationLoader.Load(environment, externalSystemMode);
    }
}












