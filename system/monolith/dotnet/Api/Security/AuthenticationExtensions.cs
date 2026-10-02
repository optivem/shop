using System.Security.Claims;
using System.Text.Json;
using Microsoft.AspNetCore.Antiforgery;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Authentication.OpenIdConnect;
using Microsoft.AspNetCore.Authorization;
using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.Protocols.OpenIdConnect;
using Microsoft.IdentityModel.Tokens;

namespace MyCompany.MyShop.Monolith.Api.Security;

public static class Roles
{
    public const string Admin = "ADMIN";
    public const string Customer = "CUSTOMER";
}

public static class AuthenticationExtensions
{
    private const string UserScheme = "ShopUser";
    private const string ChallengeScheme = "ShopChallenge";
    private const string XsrfCookie = "XSRF-TOKEN";
    private const string XsrfHeader = "X-XSRF-TOKEN";
    private const string BearerPrefix = "Bearer ";

    /// <summary>
    /// Server-side OIDC login for the UI (authorization code + PKCE, session cookie; tokens never reach
    /// browser JS) plus Bearer-JWT validation for API clients. Both resolve to the same realm-role rules.
    /// Every endpoint requires an authenticated user unless it is marked <c>[AllowAnonymous]</c>.
    /// </summary>
    public static IServiceCollection AddShopAuthentication(this IServiceCollection services)
    {
        services.AddOptions<AuthSettings>()
            .Configure<IConfiguration>((settings, configuration) =>
            {
                var from = AuthSettings.From(configuration);
                settings.IssuerUri = from.IssuerUri;
                settings.TokenUri = from.TokenUri;
                settings.JwkSetUri = from.JwkSetUri;
                settings.Audience = from.Audience;
                settings.ClientId = from.ClientId;
                settings.ClientSecret = from.ClientSecret;
            })
            .Validate(s => s.IsComplete,
                "AUTH_ISSUER_URI, AUTH_TOKEN_URI, AUTH_JWK_SET_URI, AUTH_AUDIENCE and AUTH_CLIENT_SECRET must all be configured.")
            .ValidateOnStart();

        services.AddSingleton<IJwksKeyProvider, RemoteJwksKeyProvider>();
        services.AddAntiforgery(options =>
        {
            options.HeaderName = XsrfHeader;
            options.Cookie.SameSite = SameSiteMode.Lax;
            options.Cookie.SecurePolicy = CookieSecurePolicy.SameAsRequest;
        });

        services.AddAuthentication(options =>
            {
                options.DefaultScheme = UserScheme;
                options.DefaultChallengeScheme = ChallengeScheme;
            })
            // Requests carrying a Bearer token are authenticated as API clients; everything else uses the session cookie.
            .AddPolicyScheme(UserScheme, UserScheme, options =>
                options.ForwardDefaultSelector = context => IsBearer(context.Request)
                    ? JwtBearerDefaults.AuthenticationScheme
                    : CookieAuthenticationDefaults.AuthenticationScheme)
            // API calls get a 401 problem; page requests are redirected to the Keycloak login.
            .AddPolicyScheme(ChallengeScheme, ChallengeScheme, options =>
                options.ForwardDefaultSelector = context => context.Request.Path.StartsWithSegments("/api")
                    ? JwtBearerDefaults.AuthenticationScheme
                    : OpenIdConnectDefaults.AuthenticationScheme)
            .AddCookie(CookieAuthenticationDefaults.AuthenticationScheme, options =>
            {
                options.Cookie.HttpOnly = true;
                options.Cookie.SameSite = SameSiteMode.Lax;
                options.Cookie.SecurePolicy = CookieSecurePolicy.SameAsRequest;
                options.Events.OnRedirectToAccessDenied = context =>
                    ProblemDetailSecurityHandlers.WriteForbiddenAsync(context.HttpContext);
                options.Events.OnRedirectToLogin = context =>
                    ProblemDetailSecurityHandlers.WriteUnauthorizedAsync(context.HttpContext);
            })
            .AddOpenIdConnect(OpenIdConnectDefaults.AuthenticationScheme, _ => { })
            .AddJwtBearer();

        services.AddOptions<OpenIdConnectOptions>(OpenIdConnectDefaults.AuthenticationScheme)
            .Configure<IOptions<AuthSettings>, IJwksKeyProvider>((options, authSettings, keyProvider) =>
            {
                var settings = authSettings.Value;
                options.SignInScheme = CookieAuthenticationDefaults.AuthenticationScheme;
                options.ClientId = settings.ClientId;
                options.ClientSecret = settings.ClientSecret;
                options.ResponseType = OpenIdConnectResponseType.Code;
                // Query (not form_post) so the SameSite=Lax correlation cookies survive the redirect back over plain HTTP.
                options.ResponseMode = OpenIdConnectResponseMode.Query;
                options.UsePkce = true;
                options.SaveTokens = false;
                options.GetClaimsFromUserInfoEndpoint = false;
                options.RequireHttpsMetadata = false;
                options.MapInboundClaims = false;
                options.Scope.Clear();
                options.Scope.Add("openid");
                options.Scope.Add("profile");
                options.Scope.Add("email");
                options.CorrelationCookie.SameSite = SameSiteMode.Lax;
                options.CorrelationCookie.SecurePolicy = CookieSecurePolicy.SameAsRequest;
                options.NonceCookie.SameSite = SameSiteMode.Lax;
                options.NonceCookie.SecurePolicy = CookieSecurePolicy.SameAsRequest;
                // Explicit endpoints (no discovery): the browser-facing and server-to-server URLs differ inside Docker.
                options.Configuration = new OpenIdConnectConfiguration
                {
                    Issuer = settings.IssuerUri,
                    AuthorizationEndpoint = settings.AuthorizationUri,
                    TokenEndpoint = settings.TokenUri,
                    EndSessionEndpoint = settings.EndSessionUri,
                };
                options.TokenValidationParameters = new TokenValidationParameters
                {
                    ValidateIssuerSigningKey = true,
                    RequireSignedTokens = true,
                    ValidAlgorithms = [SecurityAlgorithms.RsaSha256],
                    IssuerSigningKeyResolver = (_, _, kid, _) => keyProvider.GetSigningKeys(kid),
                    ValidateIssuer = true,
                    ValidIssuer = settings.IssuerUri,
                    ValidateAudience = true,
                    ValidAudience = settings.ClientId,
                    ValidateLifetime = true,
                    NameClaimType = "preferred_username",
                    RoleClaimType = ClaimTypes.Role,
                };
                options.Events = new OpenIdConnectEvents
                {
                    OnTokenValidated = context =>
                    {
                        MapRealmRolesToRoleClaims(context.Principal);
                        return Task.CompletedTask;
                    },
                    // Without an id_token_hint (tokens are not saved) Keycloak needs client_id to accept post_logout_redirect_uri.
                    OnRedirectToIdentityProviderForSignOut = context =>
                    {
                        context.ProtocolMessage.SetParameter("client_id", settings.ClientId);
                        return Task.CompletedTask;
                    },
                };
            });

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

    /// <summary>
    /// CSRF protection for browser sessions: hands the page an XSRF-TOKEN cookie (echoed back by fetch() as the
    /// X-XSRF-TOKEN header) and rejects state-changing requests that lack a valid token. Bearer requests carry
    /// no ambient credentials, so they are exempt.
    /// </summary>
    public static IApplicationBuilder UseShopCsrfProtection(this IApplicationBuilder app) =>
        app.Use(async (context, next) =>
        {
            if (IsBearer(context.Request) || context.User.Identity?.IsAuthenticated != true)
            {
                await next();
                return;
            }

            var antiforgery = context.RequestServices.GetRequiredService<IAntiforgery>();
            var method = context.Request.Method;
            var isSafe = HttpMethods.IsGet(method) || HttpMethods.IsHead(method)
                || HttpMethods.IsOptions(method) || HttpMethods.IsTrace(method);
            if (!isSafe)
            {
                try
                {
                    await antiforgery.ValidateRequestAsync(context);
                }
                catch (AntiforgeryValidationException)
                {
                    await ProblemDetailSecurityHandlers.WriteForbiddenAsync(context);
                    return;
                }
            }

            var tokens = antiforgery.GetAndStoreTokens(context);
            context.Response.Cookies.Append(XsrfCookie, tokens.RequestToken!, new CookieOptions
            {
                HttpOnly = false,
                SameSite = SameSiteMode.Lax,
                Secure = context.Request.IsHttps,
            });

            await next();
        });

    private static bool IsBearer(HttpRequest request) =>
        request.Headers.Authorization.ToString().StartsWith(BearerPrefix, StringComparison.OrdinalIgnoreCase);

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
