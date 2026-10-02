using System.Collections.Concurrent;
using System.Text.Json;

namespace Driver.Adapter.Shared.Client.Http;

/// <summary>
/// Acquires access tokens with the password grant (test realm only). Tokens are cached per
/// Keycloak instance and user for the lifetime of the process and refreshed shortly before expiry.
/// </summary>
public sealed class KeycloakTokenProvider
{
    private const string ClientId = "shop-system-test";
    private static readonly TimeSpan ExpiryMargin = TimeSpan.FromSeconds(30);
    private static readonly ConcurrentDictionary<string, KeycloakTokenProvider> Providers = new();
    private static readonly HttpClient SharedHttpClient = new();

    private sealed record CachedToken(string Value, DateTimeOffset ExpiresAt);

    private readonly Uri _tokenUri;
    private readonly ConcurrentDictionary<TestUser, CachedToken> _cache = new();
    private readonly SemaphoreSlim _lock = new(1, 1);

    private KeycloakTokenProvider(string keycloakBaseUrl)
    {
        _tokenUri = new Uri(keycloakBaseUrl.TrimEnd('/') + "/realms/shop/protocol/openid-connect/token");
    }

    public static KeycloakTokenProvider ForBaseUrl(string keycloakBaseUrl)
        => Providers.GetOrAdd(keycloakBaseUrl, url => new KeycloakTokenProvider(url));

    public async Task<string> GetTokenAsync(TestUser user)
    {
        if (_cache.TryGetValue(user, out var cached) && IsValid(cached))
            return cached.Value;

        await _lock.WaitAsync();
        try
        {
            if (_cache.TryGetValue(user, out cached) && IsValid(cached))
                return cached.Value;

            var fresh = await FetchTokenAsync(user);
            _cache[user] = fresh;
            return fresh.Value;
        }
        finally
        {
            _lock.Release();
        }
    }

    private static bool IsValid(CachedToken token) => DateTimeOffset.UtcNow < token.ExpiresAt;

    private async Task<CachedToken> FetchTokenAsync(TestUser user)
    {
        var form = new FormUrlEncodedContent(new Dictionary<string, string>
        {
            ["grant_type"] = "password",
            ["client_id"] = ClientId,
            ["username"] = user.Username(),
            ["password"] = user.Password()
        });

        HttpResponseMessage response;
        try
        {
            response = await SharedHttpClient.PostAsync(_tokenUri, form);
        }
        catch (HttpRequestException e)
        {
            throw new InvalidOperationException($"Failed to acquire token for {user.Username()} from {_tokenUri}", e);
        }

        var body = await response.Content.ReadAsStringAsync();
        if (!response.IsSuccessStatusCode)
        {
            throw new InvalidOperationException(
                $"Failed to acquire token for {user.Username()} from {_tokenUri}: HTTP {(int)response.StatusCode} {body}");
        }

        using var json = JsonDocument.Parse(body);
        var root = json.RootElement;
        var expiresIn = root.TryGetProperty("expires_in", out var expiresInElement) ? expiresInElement.GetInt64() : 60;
        var expiresAt = DateTimeOffset.UtcNow.AddSeconds(expiresIn) - ExpiryMargin;
        return new CachedToken(root.GetProperty("access_token").GetString()!, expiresAt);
    }
}
