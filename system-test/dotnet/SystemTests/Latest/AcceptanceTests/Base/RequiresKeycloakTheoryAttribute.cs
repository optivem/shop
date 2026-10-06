using SystemTests.TestInfrastructure.Configuration;
using Dsl.Port;
using Xunit;

namespace SystemTests.Latest.AcceptanceTests.Base;

/// <summary>
/// A <see cref="TheoryAttribute"/> for identity scenarios: the scenario is skipped when no Keycloak URL is
/// configured (a system without authentication has no identities to act as).
/// </summary>
[AttributeUsage(AttributeTargets.Method)]
public sealed class RequiresKeycloakTheoryAttribute : TheoryAttribute
{
    public RequiresKeycloakTheoryAttribute()
    {
        if (SystemConfigurationLoader.ResolveKeycloakBaseUrl(ExternalSystemMode.Stub) == null)
        {
            Skip = "Identity scenarios require KEYCLOAK_URL to be set";
        }
    }
}
