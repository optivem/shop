using Dsl.Port;
using Dsl.Core.Shared;
using SystemTests.Latest.Base;
using SystemTests.TestInfrastructure.Configuration;

namespace SystemTests.Latest.AcceptanceTests.Base;

public abstract class BaseAcceptanceTest : BaseScenarioDslTest
{
    public override async Task InitializeAsync()
    {
        var externalSystemMode = ExternalSystemMode.Stub;
        if (SystemConfigurationLoader.ResolveKeycloakBaseUrl(externalSystemMode) == null)
        {
            throw new InvalidOperationException(
                "Keycloak base URL is not configured. Set the environment variable KEYCLOAK_URL_"
                + externalSystemMode.ToString().ToUpper()
                + " (or KEYCLOAK_URL) to the Keycloak base URL, e.g. http://localhost:8180, "
                + "and start the Keycloak container before running these tests.");
        }

        await base.InitializeAsync();
    }

    protected override ExternalSystemMode? GetFixedExternalSystemMode()
    {
        return ExternalSystemMode.Stub;
    }
}
