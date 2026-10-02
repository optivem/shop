using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.Protocols;
using Microsoft.IdentityModel.Tokens;

namespace MyCompany.MyShop.Monolith.Api.Security;

/// <summary>Supplies the public keys that token signatures are verified against.</summary>
public interface IJwksKeyProvider
{
    IReadOnlyCollection<SecurityKey> GetSigningKeys(string? keyId);
}

/// <summary>
/// Fetches the identity provider's JWKS over HTTP, caches it, and re-fetches (rate-limited) when a
/// token references a key id that is not in the cached set, so key rotation is picked up.
/// </summary>
public sealed class RemoteJwksKeyProvider : IJwksKeyProvider
{
    private readonly ConfigurationManager<JsonWebKeySet> _manager;

    public RemoteJwksKeyProvider(IOptions<AuthSettings> settings)
    {
        _manager = new ConfigurationManager<JsonWebKeySet>(
            settings.Value.JwkSetUri,
            new JwksRetriever(),
            new HttpDocumentRetriever { RequireHttps = false })
        {
            RefreshInterval = TimeSpan.FromSeconds(30),
        };
    }

    public IReadOnlyCollection<SecurityKey> GetSigningKeys(string? keyId)
    {
        var keys = Find(keyId);
        if (keys.Count == 0)
        {
            _manager.RequestRefresh();
            keys = Find(keyId);
        }
        return keys;
    }

    private List<SecurityKey> Find(string? keyId)
    {
        var jwks = _manager.GetConfigurationAsync(CancellationToken.None).GetAwaiter().GetResult();
        return jwks.GetSigningKeys()
            .Where(k => keyId is null || k.KeyId == keyId)
            .ToList();
    }

    private sealed class JwksRetriever : IConfigurationRetriever<JsonWebKeySet>
    {
        public async Task<JsonWebKeySet> GetConfigurationAsync(
            string address, IDocumentRetriever retriever, CancellationToken cancel)
        {
            var json = await retriever.GetDocumentAsync(address, cancel);
            return new JsonWebKeySet(json);
        }
    }
}
