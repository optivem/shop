using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Security.Cryptography;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.IdentityModel.Tokens;
using MyCompany.MyShop.Monolith.Api.Security;
using MyCompany.MyShop.Monolith.Data;

namespace MyCompany.MyShop.Monolith.Tests;

/// <summary>
/// Hosts the monolith with an in-memory database and a locally generated signing key standing in for Keycloak,
/// so tests can mint tokens (valid, wrong audience, expired) without a live identity provider.
/// </summary>
public sealed class ShopWebApplicationFactory : WebApplicationFactory<Program>
{
    public const string Issuer = "http://keycloak.test/realms/shop";
    public const string Audience = "shop-backend";

    private readonly RsaSecurityKey _key = new(RSA.Create(2048)) { KeyId = "test-key" };

    protected override void ConfigureWebHost(IWebHostBuilder builder)
    {
        builder.UseSetting("AUTH_ISSUER_URI", Issuer);
        builder.UseSetting("AUTH_TOKEN_URI", "http://keycloak.test/token");
        builder.UseSetting("AUTH_JWK_SET_URI", "http://keycloak.test/certs");
        builder.UseSetting("AUTH_AUDIENCE", Audience);
        builder.UseSetting("AUTH_CLIENT_SECRET", "test-secret");

        builder.ConfigureServices(services =>
        {
            var descriptor = services.SingleOrDefault(
                d => d.ServiceType == typeof(DbContextOptions<AppDbContext>));
            if (descriptor != null)
                services.Remove(descriptor);
            services.AddDbContext<AppDbContext>(options => options.UseInMemoryDatabase("TestDb"));

            services.AddSingleton<IJwksKeyProvider>(new FixedKeyProvider(_key));
        });
    }

    public string CreateToken(string role, string audience = Audience, TimeSpan? lifetime = null)
    {
        var now = DateTime.UtcNow;
        var token = new JwtSecurityToken(
            issuer: Issuer,
            audience: audience,
            claims:
            [
                new Claim("sub", "test-user"),
                new Claim("preferred_username", "tester"),
                new Claim("realm_access", $"{{\"roles\":[\"{role}\"]}}", JsonClaimValueTypes.Json),
            ],
            notBefore: now.AddMinutes(-10),
            expires: now.Add(lifetime ?? TimeSpan.FromMinutes(5)),
            signingCredentials: new SigningCredentials(_key, SecurityAlgorithms.RsaSha256));
        return new JwtSecurityTokenHandler().WriteToken(token);
    }

    private sealed class FixedKeyProvider(SecurityKey key) : IJwksKeyProvider
    {
        public IReadOnlyCollection<SecurityKey> GetSigningKeys(string? keyId) => [key];
    }
}
