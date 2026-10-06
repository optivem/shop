namespace Driver.Port;

/// <summary>
/// Who an operation is performed as. A customer identity carries the zero-based position of the test customer
/// (0 = customer1, 1 = customer2, ...), so two identities are the same user exactly when they are equal.
/// </summary>
public readonly record struct UserIdentity(UserIdentityKind Kind, int CustomerIndex)
{
    /// <summary>The number of test customers available in the identity provider (customer1, customer2).</summary>
    public const int CustomerCount = 2;

    /// <summary>Customer for customer operations, admin for admin-only operations.</summary>
    public static UserIdentity Default { get; } = new(UserIdentityKind.Default, -1);

    /// <summary>No credentials at all.</summary>
    public static UserIdentity Anonymous { get; } = new(UserIdentityKind.Anonymous, -1);

    public static UserIdentity Admin { get; } = new(UserIdentityKind.Admin, -1);

    /// <summary>The test customer at the given zero-based position.</summary>
    public static UserIdentity Customer(int customerIndex)
    {
        if (customerIndex < 0 || customerIndex >= CustomerCount)
        {
            throw new ArgumentOutOfRangeException(nameof(customerIndex), customerIndex,
                $"Test customer index {customerIndex} is out of range; there are {CustomerCount} test customers");
        }

        return new UserIdentity(UserIdentityKind.Customer, customerIndex);
    }
}

public enum UserIdentityKind
{
    Default,
    Anonymous,
    Customer,
    Admin
}
