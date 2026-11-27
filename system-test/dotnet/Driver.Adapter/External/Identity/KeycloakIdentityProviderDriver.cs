using Common;
using Driver.Adapter.External.Identity.Client;
using Driver.Port;
using Driver.Port.External.Identity;
using Driver.Port.External.Identity.Dtos.Error;

namespace Driver.Adapter.External.Identity;

/// <summary>
/// Creates customers and admins in Keycloak's test realm, each with a unique username and a random password, and
/// deletes them again on dispose. The admin client signs in only when a user is first created, so
/// <see cref="GoToIdentityProviderAsync"/> needs no admin credentials.
/// The smoke check runs on every stage. A deployed QA or production stage leaves KEYCLOAK_URL unset (its backends reject the test realm's tokens), so the check falls back to KEYCLOAK_BASE_URL and checks the stage's own realm, KEYCLOAK_REALM (shop-qa, shop-prod); both default to the acceptance setup.
/// </summary>
public sealed class KeycloakIdentityProviderDriver : IIdentityProviderDriver
{
    private const string CustomerRole = "CUSTOMER";
    private const string AdminRole = "ADMIN";
    private const string CustomerUsernamePrefix = "new-customer-";
    private const string AdminUsernamePrefix = "new-admin-";

    private readonly string? _keycloakBaseUrl;
    private readonly List<string> _createdUserIds = [];
    private KeycloakAdminClient? _adminClient;

    public KeycloakIdentityProviderDriver(string? keycloakBaseUrl)
    {
        _keycloakBaseUrl = keycloakBaseUrl;
    }

    private KeycloakAdminClient Client => _adminClient ??= new KeycloakAdminClient(_keycloakBaseUrl);

    public async Task<Result<VoidValue, IdentityProviderErrorResponse>> GoToIdentityProviderAsync()
    {
        var baseUrl = OrEnv(_keycloakBaseUrl, "KEYCLOAK_BASE_URL");
        if (baseUrl == null)
        {
            return Result.Failure(new IdentityProviderErrorResponse
            {
                Message = "Keycloak base URL is not configured: set KEYCLOAK_URL (or KEYCLOAK_URL_<MODE>), or"
                    + " KEYCLOAK_BASE_URL on a deployed stage"
            });
        }
        var realm = OrEnv(null, "KEYCLOAK_REALM") ?? KeycloakAdminClient.Realm;
        return (await new KeycloakRealmClient(baseUrl, realm).CheckHealthAsync())
            .MapError(message => new IdentityProviderErrorResponse { Message = message });
    }

    private static string? OrEnv(string? value, string envName)
    {
        if (!string.IsNullOrWhiteSpace(value)) return value;
        var env = Environment.GetEnvironmentVariable(envName);
        return string.IsNullOrWhiteSpace(env) ? null : env;
    }

    public Task<UserAccount> CreateCustomerAsync(string firstName, string lastName, string? email)
        => CreateUserAsync(CustomerUsernamePrefix, CustomerRole, firstName, lastName, email);

    public Task<UserAccount> CreateAdminAsync(string firstName, string lastName, string? email)
        => CreateUserAsync(AdminUsernamePrefix, AdminRole, firstName, lastName, email);

    public UserAccount NewAccountCredentials()
        => new(CustomerUsernamePrefix + Guid.NewGuid(), Guid.NewGuid().ToString());

    public string NewAccountEmail(UserAccount account) => account.Username + "@example.com";

    public async Task DeleteOnDisposeAsync(string username)
        => _createdUserIds.Add(await Client.FindUserIdAsync(username));

    private async Task<UserAccount> CreateUserAsync(string usernamePrefix, string role, string firstName,
        string lastName, string? email)
    {
        var username = usernamePrefix + Guid.NewGuid();
        var password = Guid.NewGuid().ToString();
        var userId = await Client.CreateUserAsync(username, password, firstName, lastName,
            email ?? username + "@example.com");
        _createdUserIds.Add(userId);
        await Client.AssignRealmRoleAsync(userId, role);
        if (role == AdminRole)
        {
            // Every new user gets the realm's default role, which makes a visitor who signs up a customer (UC-013).
            // An admin is not a customer.
            await Client.RemoveDefaultRolesAsync(userId);
        }
        return new UserAccount(username, password);
    }

    public async ValueTask DisposeAsync()
    {
        var failures = new List<string>();
        foreach (var userId in _createdUserIds)
        {
            try
            {
                await Client.DeleteUserAsync(userId);
            }
            catch (InvalidOperationException e)
            {
                failures.Add(e.Message);
            }
        }
        _createdUserIds.Clear();
        _adminClient?.Dispose();
        if (failures.Count > 0)
        {
            throw new InvalidOperationException(
                "Could not delete the users created for this scenario: [" + string.Join(", ", failures) + "]");
        }
    }
}
