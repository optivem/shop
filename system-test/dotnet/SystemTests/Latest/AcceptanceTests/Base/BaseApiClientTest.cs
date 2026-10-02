using Driver.Adapter.Api.Client;
using Dsl.Port;
using Xunit;

namespace SystemTests.Latest.AcceptanceTests.Base;

/// <summary>
/// Drives the API client directly (no DSL) so a test can choose the caller's identity. Authorization
/// tests are skipped when no Keycloak URL is configured (a system without authentication).
/// </summary>
public abstract class BaseApiClientTest : BaseConfigurableTest, IAsyncLifetime
{
    protected MyShopApiClient ApiClient { get; private set; } = null!;

    protected override ExternalSystemMode? GetFixedExternalSystemMode()
    {
        return ExternalSystemMode.Stub;
    }

    public virtual Task InitializeAsync()
    {
        var configuration = LoadConfiguration();
        Skip.If(string.IsNullOrWhiteSpace(configuration.KeycloakBaseUrl),
            "Authorization tests require KEYCLOAK_URL to be set");
        ApiClient = new MyShopApiClient(configuration.MyShopApiBaseUrl, configuration.KeycloakBaseUrl);
        return Task.CompletedTask;
    }

    public virtual Task DisposeAsync()
    {
        ApiClient?.Dispose();
        return Task.CompletedTask;
    }
}
