using System.Net;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Text.Json.Nodes;

namespace Driver.Adapter.External.Identity.Client;

/// <summary>
/// Keycloak's admin REST API, for the test realm only. How it signs in depends on where Keycloak is:
/// <list type="bullet">
/// <item>A local Keycloak (localhost, the docker-compose stacks): the master realm's bootstrap admin, which the stacks
/// set to admin/admin (KC_BOOTSTRAP_ADMIN_*); KEYCLOAK_ADMIN_USERNAME / KEYCLOAK_ADMIN_PASSWORD override that.</item>
/// <item>Any other Keycloak (the shared cloud one): a confidential service-account client of the test realm itself,
/// holding only realm-management's user roles (client-credentials grant, KEYCLOAK_TEST_ADMIN_CLIENT_ID /
/// KEYCLOAK_TEST_ADMIN_CLIENT_SECRET). KEYCLOAK_TEST_ADMIN_REALM must name the test realm, and the production realm
/// is refused whatever the config says. Both checks run before any call, and the master admin is never used.</item>
/// </list>
/// </summary>
public sealed class KeycloakAdminClient : IDisposable
{
    public const string Realm = "shop";
    private const string ProductionRealm = "shop-prod";
    private const string AdminClientId = "admin-cli";
    private const string DefaultAdmin = "admin";
    private static readonly HashSet<string> LocalHosts = new(StringComparer.OrdinalIgnoreCase)
    {
        "localhost", "127.0.0.1", "[::1]", "::1"
    };

    private readonly string _baseUrl;
    private readonly TokenGrant _tokenGrant;
    private readonly HttpClient _httpClient = new();

    /// <summary>How to get an admin token: the token endpoint, its form, and what to say when Keycloak refuses it.</summary>
    private sealed record TokenGrant(string TokenUrl, Dictionary<string, string> Form, string RefusedMessage);

    public KeycloakAdminClient(string? keycloakBaseUrl)
    {
        _baseUrl = RequireUrl(keycloakBaseUrl);
        _tokenGrant = IsLocal(_baseUrl)
            ? MasterAdminGrant(_baseUrl, EnvOrDefault("KEYCLOAK_ADMIN_USERNAME"), EnvOrDefault("KEYCLOAK_ADMIN_PASSWORD"))
            : ServiceAccountGrant(_baseUrl);
    }

    /// <summary>Signs in as the given master-realm admin; a local Keycloak only.</summary>
    public KeycloakAdminClient(string? keycloakBaseUrl, string adminUsername, string adminPassword)
    {
        _baseUrl = RequireUrl(keycloakBaseUrl);
        if (!IsLocal(_baseUrl))
        {
            throw new InvalidOperationException(
                "The master-realm admin is used only for a local Keycloak (localhost), never for a shared one;"
                + $" KEYCLOAK_URL is {_baseUrl}.");
        }
        _tokenGrant = MasterAdminGrant(_baseUrl, adminUsername, adminPassword);
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

    /// <summary>
    /// Gives the user a realm role, e.g. CUSTOMER. The role is looked up among the roles the user can be given, which
    /// needs only view-users (reading the realm's roles directly needs view-realm, which the service account lacks).
    /// </summary>
    public async Task AssignRealmRoleAsync(string userId, string roleName)
    {
        var (available, availableBody) = await SendAsync(await AdminRequestAsync(HttpMethod.Get,
            "/users/" + Uri.EscapeDataString(userId) + "/role-mappings/realm/available"));
        ExpectStatus(available, availableBody, HttpStatusCode.OK, $"list the realm roles user {userId} can be given");
        var role = Parse(availableBody).AsArray()
            .FirstOrDefault(candidate => candidate?["name"]?.GetValue<string>() == roleName)
            ?? throw new InvalidOperationException(
                $"Keycloak at {_baseUrl} has no realm role {roleName} that user {userId} can be given in realm {Realm}:"
                + $" {availableBody}");
        var (mapping, mappingBody) = await SendAsync(await AdminRequestAsync(HttpMethod.Post,
            "/users/" + Uri.EscapeDataString(userId) + "/role-mappings/realm",
            new JsonArray(role.DeepClone()).ToJsonString()));
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

    /// <summary>A fresh admin token (they live for a minute, so one per call keeps it simple).</summary>
    private async Task<string> AdminTokenAsync()
    {
        var request = new HttpRequestMessage(HttpMethod.Post, _tokenGrant.TokenUrl)
        {
            Content = new FormUrlEncodedContent(_tokenGrant.Form)
        };
        var (response, body) = await SendAsync(request);
        if (response.StatusCode == HttpStatusCode.Unauthorized)
        {
            throw new InvalidOperationException(_tokenGrant.RefusedMessage);
        }
        ExpectStatus(response, body, HttpStatusCode.OK, "get an admin token");
        var token = Parse(body)["access_token"]?.GetValue<string>();
        return string.IsNullOrEmpty(token)
            ? throw new InvalidOperationException(
                $"Keycloak at {_baseUrl} sent an admin token response without access_token")
            : token;
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
        if (response.StatusCode == HttpStatusCode.Forbidden)
        {
            throw new InvalidOperationException(
                $"Keycloak at {_baseUrl} did not allow the admin account to {action} in realm {Realm} (HTTP 403). The"
                + " service account needs realm-management's manage-users, view-users and query-users, which the"
                + $" acceptance stage's deploy-keycloak job grants: {body}");
        }
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

    private static TokenGrant MasterAdminGrant(string baseUrl, string adminUsername, string adminPassword) =>
        new($"{baseUrl}/realms/master/protocol/openid-connect/token",
            new Dictionary<string, string>
            {
                ["grant_type"] = "password",
                ["client_id"] = AdminClientId,
                ["username"] = adminUsername,
                ["password"] = adminPassword
            },
            $"Keycloak at {baseUrl} refused the admin sign-in as '{adminUsername}' (HTTP 401). The local stacks use"
            + " admin/admin (KC_BOOTSTRAP_ADMIN_*); set KEYCLOAK_ADMIN_USERNAME and KEYCLOAK_ADMIN_PASSWORD if yours"
            + " differ.");

    /// <summary>
    /// The test realm's service account, for a shared Keycloak. Refuses the production realm, and any realm that
    /// KEYCLOAK_TEST_ADMIN_REALM does not name, before anything is sent.
    /// </summary>
    private static TokenGrant ServiceAccountGrant(string baseUrl)
    {
        var clientId = Environment.GetEnvironmentVariable("KEYCLOAK_TEST_ADMIN_CLIENT_ID");
        var clientSecret = Environment.GetEnvironmentVariable("KEYCLOAK_TEST_ADMIN_CLIENT_SECRET");
        var allowedRealm = Environment.GetEnvironmentVariable("KEYCLOAK_TEST_ADMIN_REALM");
        if (allowedRealm == ProductionRealm)
        {
            throw new InvalidOperationException(
                $"Refusing to create users in the production realm {ProductionRealm} of the Keycloak at {baseUrl}:"
                + $" KEYCLOAK_TEST_ADMIN_REALM must name the test realm ({Realm}). Nothing was sent to Keycloak.");
        }

        var missing = new List<string>();
        if (string.IsNullOrWhiteSpace(clientId))
        {
            missing.Add("KEYCLOAK_TEST_ADMIN_CLIENT_ID");
        }
        if (string.IsNullOrWhiteSpace(clientSecret))
        {
            missing.Add("KEYCLOAK_TEST_ADMIN_CLIENT_SECRET");
        }
        if (string.IsNullOrWhiteSpace(allowedRealm))
        {
            missing.Add("KEYCLOAK_TEST_ADMIN_REALM");
        }
        if (missing.Count > 0)
        {
            throw new InvalidOperationException(
                $"A new customer in the shared Keycloak at {baseUrl} needs the test realm's service account, but"
                + $" {string.Join(", ", missing)} is not set. Set KEYCLOAK_TEST_ADMIN_CLIENT_ID (shop-system-test-admin),"
                + $" KEYCLOAK_TEST_ADMIN_CLIENT_SECRET and KEYCLOAK_TEST_ADMIN_REALM={Realm} (the cloud acceptance stage"
                + " sets them, the secret from the KEYCLOAK_TEST_ADMIN_CLIENT_SECRET GitHub secret), or run against a"
                + " local stack.");
        }
        if (allowedRealm != Realm)
        {
            throw new InvalidOperationException(
                $"KEYCLOAK_TEST_ADMIN_REALM is '{allowedRealm}', but new customers are created in realm {Realm}, which"
                + $" it does not allow. Set KEYCLOAK_TEST_ADMIN_REALM={Realm}. Nothing was sent to the Keycloak at"
                + $" {baseUrl}.");
        }

        return new TokenGrant($"{baseUrl}/realms/{Realm}/protocol/openid-connect/token",
            new Dictionary<string, string>
            {
                ["grant_type"] = "client_credentials",
                ["client_id"] = clientId!,
                ["client_secret"] = clientSecret!
            },
            $"Keycloak at {baseUrl} refused the service account '{clientId}' of realm {Realm} (HTTP 401)."
            + " KEYCLOAK_TEST_ADMIN_CLIENT_SECRET must match the client's secret, which the acceptance stage's"
            + " deploy-keycloak job sets from the GitHub secret of the same name; check that secret and re-run the"
            + " stage.");
    }

    private static string RequireUrl(string? keycloakBaseUrl)
    {
        if (string.IsNullOrWhiteSpace(keycloakBaseUrl))
        {
            throw new InvalidOperationException(
                "A new customer needs Keycloak: set KEYCLOAK_URL (or KEYCLOAK_URL_<MODE>) to the stack's Keycloak,"
                + " e.g. http://localhost:8292.");
        }
        if (!Uri.TryCreate(keycloakBaseUrl, UriKind.Absolute, out _))
        {
            throw new InvalidOperationException($"KEYCLOAK_URL is not an absolute URL: {keycloakBaseUrl}");
        }
        return keycloakBaseUrl.TrimEnd('/');
    }

    private static bool IsLocal(string keycloakBaseUrl) => LocalHosts.Contains(new Uri(keycloakBaseUrl).Host);

    private static string EnvOrDefault(string name)
    {
        var value = Environment.GetEnvironmentVariable(name);
        return string.IsNullOrWhiteSpace(value) ? DefaultAdmin : value;
    }
}
