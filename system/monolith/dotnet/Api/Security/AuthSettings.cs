namespace MyCompany.MyShop.Monolith.Api.Security;

/// <summary>
/// Identity-provider settings. Each value is read from an environment variable first
/// (AUTH_ISSUER_URI, AUTH_TOKEN_URI, AUTH_JWK_SET_URI, AUTH_AUDIENCE, AUTH_CLIENT_ID, AUTH_CLIENT_SECRET),
/// with the <c>Auth</c> configuration section as the fallback.
/// </summary>
public sealed class AuthSettings
{
    private const string OidcBase = "/protocol/openid-connect";
    private const string DefaultClientId = "shop-monolith";

    /// <summary>Expected <c>iss</c> claim and the browser-facing realm URL.</summary>
    public string IssuerUri { get; set; } = string.Empty;

    /// <summary>Server-to-server token endpoint (may be an internal address).</summary>
    public string TokenUri { get; set; } = string.Empty;

    /// <summary>Where the realm's signing keys (JWKS) are fetched from (may be an internal address).</summary>
    public string JwkSetUri { get; set; } = string.Empty;

    /// <summary>Expected <c>aud</c> claim of Bearer access tokens.</summary>
    public string Audience { get; set; } = string.Empty;

    /// <summary>Confidential client used for the browser (server-side) login.</summary>
    public string ClientId { get; set; } = DefaultClientId;

    public string ClientSecret { get; set; } = string.Empty;

    public string AuthorizationUri => IssuerUri.TrimEnd('/') + OidcBase + "/auth";

    public string EndSessionUri => IssuerUri.TrimEnd('/') + OidcBase + "/logout";

    public static AuthSettings From(IConfiguration configuration)
    {
        var clientId = Read(configuration, "AUTH_CLIENT_ID", "Auth:ClientId");
        return new AuthSettings
        {
            IssuerUri = Read(configuration, "AUTH_ISSUER_URI", "Auth:IssuerUri"),
            TokenUri = Read(configuration, "AUTH_TOKEN_URI", "Auth:TokenUri"),
            JwkSetUri = Read(configuration, "AUTH_JWK_SET_URI", "Auth:JwkSetUri"),
            Audience = Read(configuration, "AUTH_AUDIENCE", "Auth:Audience"),
            ClientId = string.IsNullOrWhiteSpace(clientId) ? DefaultClientId : clientId,
            ClientSecret = Read(configuration, "AUTH_CLIENT_SECRET", "Auth:ClientSecret"),
        };
    }

    public bool IsComplete =>
        !string.IsNullOrWhiteSpace(IssuerUri)
        && !string.IsNullOrWhiteSpace(TokenUri)
        && !string.IsNullOrWhiteSpace(JwkSetUri)
        && !string.IsNullOrWhiteSpace(Audience)
        && !string.IsNullOrWhiteSpace(ClientSecret);

    private static string Read(IConfiguration configuration, string envName, string configKey) =>
        configuration[envName] ?? configuration[configKey] ?? string.Empty;
}
