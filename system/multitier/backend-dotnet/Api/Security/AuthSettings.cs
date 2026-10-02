namespace MyCompany.MyShop.Backend.Api.Security;

/// <summary>
/// Identity-provider settings used to validate bearer tokens. Each value is read from an
/// environment variable first (AUTH_ISSUER_URI, AUTH_JWK_SET_URI, AUTH_AUDIENCE), with the
/// <c>Auth</c> configuration section as the fallback.
/// </summary>
public sealed class AuthSettings
{
    /// <summary>Expected <c>iss</c> claim (the externally visible realm URL).</summary>
    public string IssuerUri { get; set; } = string.Empty;

    /// <summary>Where the realm's signing keys (JWKS) are fetched from (may be an internal address).</summary>
    public string JwkSetUri { get; set; } = string.Empty;

    /// <summary>Expected <c>aud</c> claim.</summary>
    public string Audience { get; set; } = string.Empty;

    public static AuthSettings From(IConfiguration configuration) => new()
    {
        IssuerUri = Read(configuration, "AUTH_ISSUER_URI", "Auth:IssuerUri"),
        JwkSetUri = Read(configuration, "AUTH_JWK_SET_URI", "Auth:JwkSetUri"),
        Audience = Read(configuration, "AUTH_AUDIENCE", "Auth:Audience"),
    };

    public bool IsComplete =>
        !string.IsNullOrWhiteSpace(IssuerUri)
        && !string.IsNullOrWhiteSpace(JwkSetUri)
        && !string.IsNullOrWhiteSpace(Audience);

    private static string Read(IConfiguration configuration, string envName, string configKey) =>
        configuration[envName] ?? configuration[configKey] ?? string.Empty;
}
