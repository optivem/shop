using Driver.Adapter.External.Identity.Client;
using Driver.Port;
using Driver.Port.External.Identity;

namespace Driver.Adapter.External.Identity;

/// <summary>
/// Creates customers and admins in Keycloak's test realm, each with a unique username and a random password, and
/// deletes them again on dispose.
/// </summary>
public sealed class KeycloakIdentityProviderDriver : IIdentityProviderDriver
{
    private const string CustomerRole = "CUSTOMER";
    private const string AdminRole = "ADMIN";
    private const string CustomerUsernamePrefix = "new-customer-";
    private const string AdminUsernamePrefix = "new-admin-";

    private readonly KeycloakAdminClient _client;
    private readonly List<string> _createdUserIds = [];

    public KeycloakIdentityProviderDriver(string? keycloakBaseUrl)
    {
        _client = new KeycloakAdminClient(keycloakBaseUrl);
    }

    public Task<UserAccount> CreateCustomerAsync(string firstName, string lastName, string? email)
        => CreateUserAsync(CustomerUsernamePrefix, CustomerRole, firstName, lastName, email);

    public Task<UserAccount> CreateAdminAsync(string firstName, string lastName, string? email)
        => CreateUserAsync(AdminUsernamePrefix, AdminRole, firstName, lastName, email);

    public UserAccount NewAccountCredentials()
        => new(CustomerUsernamePrefix + Guid.NewGuid(), Guid.NewGuid().ToString());

    public string NewAccountEmail(UserAccount account) => account.Username + "@example.com";

    public async Task DeleteOnDisposeAsync(string username)
        => _createdUserIds.Add(await _client.FindUserIdAsync(username));

    private async Task<UserAccount> CreateUserAsync(string usernamePrefix, string role, string firstName,
        string lastName, string? email)
    {
        var username = usernamePrefix + Guid.NewGuid();
        var password = Guid.NewGuid().ToString();
        var userId = await _client.CreateUserAsync(username, password, firstName, lastName,
            email ?? username + "@example.com");
        _createdUserIds.Add(userId);
        await _client.AssignRealmRoleAsync(userId, role);
        if (role == AdminRole)
        {
            // Every new user gets the realm's default role, which makes a visitor who signs up a customer (UC-013).
            // An admin is not a customer.
            await _client.RemoveDefaultRolesAsync(userId);
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
                await _client.DeleteUserAsync(userId);
            }
            catch (InvalidOperationException e)
            {
                failures.Add(e.Message);
            }
        }
        _createdUserIds.Clear();
        _client.Dispose();
        if (failures.Count > 0)
        {
            throw new InvalidOperationException(
                "Could not delete the users created for this scenario: [" + string.Join(", ", failures) + "]");
        }
    }
}
