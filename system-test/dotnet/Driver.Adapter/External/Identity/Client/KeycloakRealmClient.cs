using System.Net;
using Common;

namespace Driver.Adapter.External.Identity.Client;

/// <summary>
/// A realm's public endpoints, which need no sign-in. Separate from <see cref="KeycloakAdminClient"/>, so
/// checking that Keycloak is up never needs admin credentials.
/// </summary>
public sealed class KeycloakRealmClient
{
    private const int MaxBodyInMessage = 300;
    private static readonly HttpClient SharedHttpClient = new(new SocketsHttpHandler
    {
        ConnectTimeout = TimeSpan.FromSeconds(10),
        PooledConnectionLifetime = TimeSpan.FromMinutes(1)
    })
    {
        Timeout = TimeSpan.FromSeconds(30)
    };

    private readonly string _baseUrl;
    private readonly string _realm;

    public KeycloakRealmClient(string keycloakBaseUrl, string realm)
    {
        _realm = realm;
        _baseUrl = keycloakBaseUrl.TrimEnd('/');
    }

    /// <summary>
    /// The realm's OpenID discovery document answers 200 once Keycloak has finished bootstrapping and serves the
    /// realm; during the bootstrap it answers 503.
    /// </summary>
    public async Task<Result<VoidValue, string>> CheckHealthAsync()
    {
        var url = $"{_baseUrl}/realms/{_realm}/.well-known/openid-configuration";
        try
        {
            using var response = await SharedHttpClient.GetAsync(url);
            if (response.StatusCode == HttpStatusCode.OK)
            {
                return Result.Success<string>();
            }
            var body = await response.Content.ReadAsStringAsync();
            var hint = response.StatusCode == HttpStatusCode.ServiceUnavailable
                ? " (503: Keycloak is still bootstrapping)"
                : "";
            return Result.Failure($"Keycloak realm {_realm} at {url} answered HTTP"
                + $" {(int)response.StatusCode}{hint}: {Truncate(body)}");
        }
        catch (HttpRequestException e)
        {
            return Result.Failure($"Keycloak at {url} could not be reached: {e.Message}");
        }
        catch (TaskCanceledException)
        {
            return Result.Failure($"Keycloak at {url} did not answer within {SharedHttpClient.Timeout.TotalSeconds} s");
        }
    }

    private static string Truncate(string body)
        => body.Length <= MaxBodyInMessage ? body : body[..MaxBodyInMessage] + "...";
}
