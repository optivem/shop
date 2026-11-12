using Driver.Port;

namespace Driver.Adapter.Shared.Client.Http;

/// <summary>
/// Credentials the drivers sign in with: the test realm's shared users (the realm import defines them; they exist
/// in the test realm only), or a customer account created for the scenario. Equal exactly when the username and
/// password are.
/// </summary>
public sealed record TestUser
{
    public static TestUser Customer { get; } = new("customer1", "customer1-test-password");
    public static TestUser Customer2 { get; } = new("customer2", "customer2-test-password");
    public static TestUser Admin { get; } = new("admin1", "admin1-test-password");

    private TestUser(string username, string password)
    {
        Username = username;
        Password = password;
    }

    public string Username { get; }

    public string Password { get; }

    /// <summary>The shared test customer at the given zero-based position (0 = customer1).</summary>
    public static TestUser CustomerAt(int index) => index switch
    {
        0 => Customer,
        1 => Customer2,
        _ => throw new ArgumentOutOfRangeException(nameof(index), index, $"No test customer at index {index}")
    };

    /// <summary>The user behind a customer identity: a shared test customer, or the account created for the scenario.</summary>
    public static TestUser ForCustomer(UserIdentity identity)
    {
        var account = identity.Account;
        return account != null ? new TestUser(account.Username, account.Password) : CustomerAt(identity.CustomerIndex);
    }

    public override string ToString() => Username;
}
