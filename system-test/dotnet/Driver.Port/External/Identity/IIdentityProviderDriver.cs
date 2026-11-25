namespace Driver.Port.External.Identity;

/// <summary>
/// The identity provider's test realm, for scenarios that need a user of their own: a customer (no saved profile, no
/// orders) or an admin. Unlike the system drivers it arranges, never asserts, so a failure is thrown rather than
/// returned. Disposing it removes every account it created.
/// </summary>
public interface IIdentityProviderDriver : IAsyncDisposable
{
    /// <summary>Creates a new customer sign-in account with this name and email, and the customer role.</summary>
    /// <param name="email">null for a unique address derived from the generated username</param>
    Task<UserAccount> CreateCustomerAsync(string firstName, string lastName, string? email);

    /// <summary>Creates a new admin sign-in account with this name and email, and the admin role.</summary>
    /// <param name="email">null for a unique address derived from the generated username</param>
    Task<UserAccount> CreateAdminAsync(string firstName, string lastName, string? email);
}
