using System.Security.Claims;
using System.Text.Json;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Authorization;
using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.Tokens;

namespace MyCompany.MyShop.Backend.Api.Security;

public static class Roles
{
    public const string Admin = "ADMIN";
    public const string Customer = "CUSTOMER";
}

public static class AuthenticationExtensions
{
    /// <summary>
    /// Configures the API as an OAuth2 resource server: bearer tokens are validated for signature
    /// (against the identity provider's JWKS), issuer, audience and expiry, and the realm roles in
    /// <c>realm_access.roles</c> become role claims. Every endpoint requires an authenticated user
    /// unless it is explicitly marked <c>[AllowAnonymous]</c> (secure by default).
    /// </summary>
    public static IServiceCollection AddShopAuthentication(this IServiceCollection services)
    {
        services.AddOptions<AuthSettings>()
            .Configure<IConfiguration>((settings, configuration) =>
            {
                var from = AuthSettings.From(configuration);
                settings.IssuerUri = from.IssuerUri;
                settings.JwkSetUri = from.JwkSetUri;
                settings.Audience = from.Audience;
            })
            .Validate(s => s.IsComplete,
                "AUTH_ISSUER_URI, AUTH_JWK_SET_URI and AUTH_AUDIENCE must all be configured.")
            .ValidateOnStart();

        services.AddSingleton<IJwksKeyProvider, RemoteJwksKeyProvider>();

        services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
            .AddJwtBearer();

        services.AddOptions<JwtBearerOptions>(JwtBearerDefaults.AuthenticationScheme)
            .Configure<IOptions<AuthSettings>, IJwksKeyProvider>((options, authSettings, keyProvider) =>
            {
                var settings = authSettings.Value;
                options.MapInboundClaims = false;
                options.RequireHttpsMetadata = false;
                options.TokenValidationParameters = new TokenValidationParameters
                {
                    ValidateIssuerSigningKey = true,
                    RequireSignedTokens = true,
                    ValidAlgorithms = [SecurityAlgorithms.RsaSha256],
                    IssuerSigningKeyResolver = (_, _, kid, _) => keyProvider.GetSigningKeys(kid),
                    ValidateIssuer = true,
                    ValidIssuer = settings.IssuerUri,
                    ValidateAudience = true,
                    ValidAudience = settings.Audience,
                    ValidateLifetime = true,
                    RequireExpirationTime = true,
                    ClockSkew = TimeSpan.FromSeconds(30),
                    NameClaimType = "preferred_username",
                    RoleClaimType = ClaimTypes.Role,
                };
                options.Events = new JwtBearerEvents
                {
                    OnTokenValidated = context =>
                    {
                        MapRealmRolesToRoleClaims(context.Principal);
                        return Task.CompletedTask;
                    },
                    OnChallenge = async context =>
                    {
                        context.HandleResponse();
                        await ProblemDetailSecurityHandlers.WriteUnauthorizedAsync(context.HttpContext);
                    },
                    OnForbidden = context =>
                        ProblemDetailSecurityHandlers.WriteForbiddenAsync(context.HttpContext),
                };
            });

        services.AddAuthorizationBuilder()
            .SetFallbackPolicy(new AuthorizationPolicyBuilder().RequireAuthenticatedUser().Build());

        return services;
    }

    private static void MapRealmRolesToRoleClaims(ClaimsPrincipal? principal)
    {
        if (principal?.Identity is not ClaimsIdentity identity)
        {
            return;
        }

        var realmAccess = identity.FindFirst("realm_access")?.Value;
        if (string.IsNullOrEmpty(realmAccess))
        {
            return;
        }

        using var document = JsonDocument.Parse(realmAccess);
        if (document.RootElement.ValueKind != JsonValueKind.Object
            || !document.RootElement.TryGetProperty("roles", out var roles)
            || roles.ValueKind != JsonValueKind.Array)
        {
            return;
        }

        foreach (var role in roles.EnumerateArray())
        {
            if (role.ValueKind == JsonValueKind.String && role.GetString() is { } name)
            {
                identity.AddClaim(new Claim(ClaimTypes.Role, name));
            }
        }
    }
}
