using System.Net;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Text.Json.Nodes;

namespace Driver.Adapter.External.Identity.Client;

/// <summary>
/// Keycloak's admin REST API, for the test realm of a local Keycloak only (the docker-compose stacks). It signs in
/// to the master realm as the bootstrap admin, which the stacks set to admin/admin (KC_BOOTSTRAP_ADMIN_*);
/// KEYCLOAK_ADMIN_USERNAME / KEYCLOAK_ADMIN_PASSWORD override that. A Keycloak that is not on this machine is
/// refused before any call, so the shared cloud Keycloak and its QA/production realms are never touched.
/// </summary>
public sealed class KeycloakAdminClient : IDisposable
{
    public const string Realm = "shop";
    private const string AdminClientId = "admin-cli";
    private const string DefaultAdmin = "admin";
    private static readonly HashSet<string> LocalHosts = new(StringComparer.OrdinalIgnoreCase)
    {
        "localhost", "127.0.0.1", "[::1]", "::1"
    };

    private readonly string _baseUrl;
    private readonly string _adminUsername;
    private readonly string _adminPassword;
    private readonly HttpClient _httpClient = new();

    public KeycloakAdminClient(string? keycloakBaseUrl)
        : this(keycloakBaseUrl, EnvOrDefault("KEYCLOAK_ADMIN_USERNAME"), EnvOrDefault("KEYCLOAK_ADMIN_PASSWORD"))
    {
    }

    public KeycloakAdminClient(string? keycloakBaseUrl, string adminUsername, string adminPassword)
    {
        _baseUrl = RequireLocal(keycloakBaseUrl);
        _adminUsername = adminUsername;
        _adminPassword = adminPassword;
    }

    /// <summary>Creates an enabled user with a permanent password and returns its id.</summary>
    public async Task<string> CreateUserAsync(string username, string password, string firstName, string lastName, string email)
    {
        var user = new JsonObject
        {
            ["username"] = username,
            ["enabled"] = true,
            ["firstName"] = firstName,
            ["lastName"] = lastName,
            ["email"] = email,
            ["emailVerified"] = true,
            ["requiredActions"] = new JsonArray(),
            ["credentials"] = new JsonArray(new JsonObject
            {
                ["type"] = "password",
                ["value"] = password,
                ["temporary"] = false
            })
        };
        var (response, body) = await SendAsync(await AdminRequestAsync(HttpMethod.Post, "/users", user.ToJsonString()));
        ExpectStatus(response, body, HttpStatusCode.Created, $"create user {username}");
        var location = response.Headers.Location
            ?? throw new InvalidOperationException(
                $"Keycloak created user {username} in realm {Realm} but sent no Location header");
        var path = location.OriginalString;
        return path[(path.LastIndexOf('/') + 1)..];
    }

    /// <summary>Gives the user a realm role, e.g. CUSTOMER.</summary>
    public async Task AssignRealmRoleAsync(string userId, string roleName)
    {
        var (roleResponse, roleBody) = await SendAsync(
            await AdminRequestAsync(HttpMethod.Get, "/roles/" + Uri.EscapeDataString(roleName)));
        ExpectStatus(roleResponse, roleBody, HttpStatusCode.OK, $"read realm role {roleName}");
        var role = Parse(roleBody);
        var (mapping, mappingBody) = await SendAsync(await AdminRequestAsync(HttpMethod.Post,
            "/users/" + Uri.EscapeDataString(userId) + "/role-mappings/realm", new JsonArray(role).ToJsonString()));
        ExpectStatus(mapping, mappingBody, HttpStatusCode.NoContent, $"give user {userId} the realm role {roleName}");
    }

    /// <summary>Deletes the user; one that is already gone counts as deleted.</summary>
    public async Task DeleteUserAsync(string userId)
    {
        var (response, body) = await SendAsync(
            await AdminRequestAsync(HttpMethod.Delete, "/users/" + Uri.EscapeDataString(userId)));
        if (response.StatusCode != HttpStatusCode.NotFound)
        {
            ExpectStatus(response, body, HttpStatusCode.NoContent, $"delete user {userId}");
        }
    }

    public void Dispose() => _httpClient.Dispose();

    private async Task<HttpRequestMessage> AdminRequestAsync(HttpMethod method, string path, string? json = null)
    {
        var request = new HttpRequestMessage(method, $"{_baseUrl}/admin/realms/{Realm}{path}");
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", await AdminTokenAsync());
        if (json != null)
        {
            request.Content = new StringContent(json, Encoding.UTF8, "application/json");
        }
        return request;
    }

    /// <summary>A fresh master-realm admin token (they live for a minute, so one per call keeps it simple).</summary>
    private async Task<string> AdminTokenAsync()
    {
        var request = new HttpRequestMessage(HttpMethod.Post, $"{_baseUrl}/realms/master/protocol/openid-connect/token")
        {
            Content = new FormUrlEncodedContent(new Dictionary<string, string>
            {
                ["grant_type"] = "password",
                ["client_id"] = AdminClientId,
                ["username"] = _adminUsername,
                ["password"] = _adminPassword
            })
        };
        var (response, body) = await SendAsync(request);
        if (response.StatusCode == HttpStatusCode.Unauthorized)
        {
            throw new InvalidOperationException(
                $"Keycloak at {_baseUrl} refused the admin sign-in as '{_adminUsername}' (HTTP 401). The local stacks"
                + " use admin/admin (KC_BOOTSTRAP_ADMIN_*); set KEYCLOAK_ADMIN_USERNAME and KEYCLOAK_ADMIN_PASSWORD if"
                + " yours differ.");
        }
        ExpectStatus(response, body, HttpStatusCode.OK, "get an admin token");
        return Parse(body)["access_token"]?.GetValue<string>()
            ?? throw new InvalidOperationException(
                $"Keycloak at {_baseUrl} sent an admin token response without access_token: {body}");
    }

    private async Task<(HttpResponseMessage Response, string Body)> SendAsync(HttpRequestMessage request)
    {
        using (request)
        {
            try
            {
                var response = await _httpClient.SendAsync(request);
                var body = await response.Content.ReadAsStringAsync();
                return (response, body);
            }
            catch (Exception e) when (e is HttpRequestException or TaskCanceledException)
            {
                throw new InvalidOperationException(
                    $"Keycloak admin call {request.Method} {request.RequestUri} failed: {e.Message}", e);
            }
        }
    }

    private void ExpectStatus(HttpResponseMessage response, string body, HttpStatusCode expected, string action)
    {
        if (response.StatusCode != expected)
        {
            throw new InvalidOperationException(
                $"Keycloak at {_baseUrl} could not {action} in realm {Realm}: HTTP {(int)response.StatusCode} {body}");
        }
    }

    private JsonNode Parse(string body)
    {
        try
        {
            return JsonNode.Parse(body)
                ?? throw new InvalidOperationException($"Keycloak at {_baseUrl} sent malformed JSON: {body}");
        }
        catch (JsonException e)
        {
            throw new InvalidOperationException($"Keycloak at {_baseUrl} sent malformed JSON: {body}", e);
        }
    }

    private static string RequireLocal(string? keycloakBaseUrl)
    {
        if (string.IsNullOrWhiteSpace(keycloakBaseUrl))
        {
            throw new InvalidOperationException(
                "A new customer needs Keycloak: set KEYCLOAK_URL (or KEYCLOAK_URL_<MODE>) to the local stack's"
                + " Keycloak, e.g. http://localhost:8192.");
        }

        var host = Uri.TryCreate(keycloakBaseUrl, UriKind.Absolute, out var uri) ? uri.Host : null;
        if (host == null || !LocalHosts.Contains(host))
        {
            throw new InvalidOperationException(
                "A new customer is created only in a local Keycloak (localhost), never in a shared one; KEYCLOAK_URL is "
                + $"{keycloakBaseUrl}. Run this scenario against a local stack.");
        }

        return keycloakBaseUrl.TrimEnd('/');
    }

    private static string EnvOrDefault(string name)
    {
        var value = Environment.GetEnvironmentVariable(name);
        return string.IsNullOrWhiteSpace(value) ? DefaultAdmin : value;
    }
}
