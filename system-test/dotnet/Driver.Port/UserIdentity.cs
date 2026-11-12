namespace Driver.Port;

/// <summary>
/// Who an operation is performed as. A shared test customer carries its zero-based position (0 = customer1,
/// 1 = customer2, ...); a customer created for the scenario carries its account instead. Two identities are the
/// same user exactly when they are equal.
/// </summary>
public readonly record struct UserIdentity(UserIdentityKind Kind, int CustomerIndex, CustomerAccount? Account = null)
{
    /// <summary>The number of shared test customers in the identity provider (customer1, customer2).</summary>
    public const int CustomerCount = 2;

    /// <summary>Customer for customer operations, admin for admin-only operations.</summary>
    public static UserIdentity Default { get; } = new(UserIdentityKind.Default, -1);

    /// <summary>No credentials at all.</summary>
    public static UserIdentity Anonymous { get; } = new(UserIdentityKind.Anonymous, -1);

    public static UserIdentity Admin { get; } = new(UserIdentityKind.Admin, -1);

    /// <summary>
    /// The sign-in username of this customer identity (customer1, customer2, ..., or the created account's), as the
    /// identity provider's test realm knows it; used where the system takes a customer by username (the admin
    /// customer filter).
    /// </summary>
    public string Username()
    {
        if (Kind != UserIdentityKind.Customer)
        {
            throw new InvalidOperationException($"Only a customer identity has a customer username; this is {Kind}");
        }

        return Account != null ? Account.Username : $"customer{CustomerIndex + 1}";
    }

    /// <summary>The shared test customer at the given zero-based position.</summary>
    public static UserIdentity Customer(int customerIndex)
    {
        if (customerIndex < 0 || customerIndex >= CustomerCount)
        {
            throw new ArgumentOutOfRangeException(nameof(customerIndex), customerIndex,
                $"Test customer index {customerIndex} is out of range; there are {CustomerCount} test customers");
        }

        return new UserIdentity(UserIdentityKind.Customer, customerIndex);
    }

    /// <summary>A customer whose account was created for this scenario.</summary>
    public static UserIdentity Customer(CustomerAccount account)
    {
        if (account == null)
        {
            throw new ArgumentNullException(nameof(account), "A created customer needs its account");
        }

        return new UserIdentity(UserIdentityKind.Customer, -1, account);
    }
}

public enum UserIdentityKind
{
    Default,
    Anonymous,
    Customer,
    Admin
}
