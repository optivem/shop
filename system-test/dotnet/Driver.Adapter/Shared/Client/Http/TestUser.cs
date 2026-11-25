using Driver.Port;

namespace Driver.Adapter.Shared.Client.Http;

/// <summary>
/// Credentials the drivers sign in with: the test realm's shared customers (the realm import defines them; they exist
/// in the test realm only), or a customer or admin account created for the scenario. Equal exactly when the username
/// and password are.
/// </summary>
public sealed record TestUser
{
    public static TestUser Customer { get; } = new("customer1", "customer1-test-password", false);
    public static TestUser Customer2 { get; } = new("customer2", "customer2-test-password", false);

    private TestUser(string username, string password, bool isAdmin)
    {
        Username = username;
        Password = password;
        IsAdmin = isAdmin;
    }

    public string Username { get; }

    public string Password { get; }

    /// <summary>Whether this user has the admin role.</summary>
    public bool IsAdmin { get; }

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
        return account != null ? new TestUser(account.Username, account.Password, false) : CustomerAt(identity.CustomerIndex);
    }

    /// <summary>The user behind a customer or admin identity.</summary>
    public static TestUser For(UserIdentity identity) => identity.Kind switch
    {
        UserIdentityKind.Customer => ForCustomer(identity),
        UserIdentityKind.Admin => new TestUser(identity.Account!.Username, identity.Account.Password, true),
        _ => throw new ArgumentOutOfRangeException(nameof(identity), identity.Kind, $"No user signs in as {identity.Kind}")
    };

    public bool Equals(TestUser? other) => other is not null && Username == other.Username && Password == other.Password;

    public override int GetHashCode() => HashCode.Combine(Username, Password);

    public override string ToString() => Username;
}
