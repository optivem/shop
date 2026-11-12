using Driver.Adapter.External.Identity.Client;
using Driver.Port;
using Driver.Port.External.Identity;

namespace Driver.Adapter.External.Identity;

/// <summary>
/// Creates customers in Keycloak's test realm, each with a unique username and a random password, and
/// deletes them again on dispose.
/// </summary>
public sealed class KeycloakIdentityProviderDriver : IIdentityProviderDriver
{
    private const string CustomerRole = "CUSTOMER";
    private const string UsernamePrefix = "new-customer-";

    private readonly KeycloakAdminClient _client;
    private readonly List<string> _createdUserIds = [];

    public KeycloakIdentityProviderDriver(string? keycloakBaseUrl)
    {
        _client = new KeycloakAdminClient(keycloakBaseUrl);
    }

    public async Task<CustomerAccount> CreateCustomerAsync(string firstName, string lastName, string? email)
    {
        var username = UsernamePrefix + Guid.NewGuid();
        var password = Guid.NewGuid().ToString();
        var userId = await _client.CreateUserAsync(username, password, firstName, lastName,
            email ?? username + "@example.com");
        _createdUserIds.Add(userId);
        await _client.AssignRealmRoleAsync(userId, CustomerRole);
        return new CustomerAccount(username, password);
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
                "Could not delete the customers created for this scenario: [" + string.Join(", ", failures) + "]");
        }
    }
}
