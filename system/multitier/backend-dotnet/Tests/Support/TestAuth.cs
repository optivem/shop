using System.Security.Claims;
using System.Security.Cryptography;
using Microsoft.AspNetCore.Hosting;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;
using Microsoft.IdentityModel.JsonWebTokens;
using Microsoft.IdentityModel.Tokens;
using MyCompany.MyShop.Backend.Api.Security;

namespace MyCompany.MyShop.Backend.Tests.Support;

/// <summary>
/// Lets the real authentication pipeline (signature, issuer, audience, expiry checks, role mapping,
/// error bodies) run without an identity provider: the test mints its own signed tokens and the
/// backend is pointed at the matching public key instead of the JWKS endpoint.
/// </summary>
public static class TestAuth
{
    public const string Issuer = "http://localhost:8291/realms/shop";
    public const string Audience = "shop-backend";

    private static readonly RsaSecurityKey SigningKey = NewKey("test-key");

    public static string AdminToken => CreateToken(["ADMIN"]);

    public static string CustomerToken => CreateToken(["CUSTOMER"]);

    /// <summary>Sees every order (ADMIN) and may place orders (CUSTOMER); used where one caller must do both.</summary>
    public static string AdminAndCustomerToken => CreateToken(["ADMIN", "CUSTOMER"]);

    public static string CustomerTokenFor(string subject) => CreateToken(["CUSTOMER"], subject: subject);

    /// <summary>Points the backend at the test key and the test issuer / audience.</summary>
    public static IWebHostBuilder UseTestAuth(this IWebHostBuilder builder)
    {
        builder.UseSetting("AUTH_ISSUER_URI", Issuer);
        builder.UseSetting("AUTH_JWK_SET_URI", "http://localhost:8291/realms/shop/protocol/openid-connect/certs");
        builder.UseSetting("AUTH_AUDIENCE", Audience);
        builder.ConfigureServices(services =>
        {
            services.RemoveAll<IJwksKeyProvider>();
            services.AddSingleton<IJwksKeyProvider>(new StaticKeyProvider(SigningKey));
        });
        return builder;
    }

    public static string CreateToken(
        string[] roles,
        string issuer = Issuer,
        string audience = Audience,
        DateTime? expires = null,
        SecurityKey? signingKey = null,
        string subject = "test-user")
    {
        var realmAccess = new Dictionary<string, object> { ["roles"] = roles };
        var now = DateTime.UtcNow;
        var descriptor = new SecurityTokenDescriptor
        {
            Issuer = issuer,
            Audience = audience,
            NotBefore = now.AddMinutes(-10),
            IssuedAt = now.AddMinutes(-10),
            Expires = expires ?? now.AddHours(1),
            Subject = new ClaimsIdentity([new Claim("sub", subject), new Claim("preferred_username", subject)]),
            Claims = new Dictionary<string, object> { ["realm_access"] = realmAccess },
            SigningCredentials = new SigningCredentials(signingKey ?? SigningKey, SecurityAlgorithms.RsaSha256),
        };
        return new JsonWebTokenHandler().CreateToken(descriptor);
    }

    /// <summary>A token signed with a key the backend does not trust.</summary>
    public static string CreateTokenSignedByStranger(string[] roles) =>
        CreateToken(roles, signingKey: NewKey("test-key"));

    /// <summary>A token with <c>alg: none</c> (no signature).</summary>
    public static string CreateUnsignedToken(string[] roles)
    {
        var payload = System.Text.Json.JsonSerializer.Serialize(new Dictionary<string, object>
        {
            ["iss"] = Issuer,
            ["aud"] = Audience,
            ["exp"] = DateTimeOffset.UtcNow.AddHours(1).ToUnixTimeSeconds(),
            ["realm_access"] = new Dictionary<string, object> { ["roles"] = roles },
        });
        var header = Base64UrlEncoder.Encode("{\"alg\":\"none\",\"typ\":\"JWT\"}");
        return header + "." + Base64UrlEncoder.Encode(payload) + ".";
    }

    private static RsaSecurityKey NewKey(string keyId) => new(RSA.Create(2048)) { KeyId = keyId };

    private sealed class StaticKeyProvider(SecurityKey key) : IJwksKeyProvider
    {
        public IReadOnlyCollection<SecurityKey> GetSigningKeys(string? keyId) =>
            keyId is null || keyId == key.KeyId ? [key] : [];
    }
}
