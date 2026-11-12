namespace Driver.Port.External.Identity;

/// <summary>
/// The identity provider's test realm, for scenarios that need a customer of their own (no saved profile, no
/// orders). Unlike the system drivers it arranges, never asserts, so a failure is thrown rather than returned.
/// Disposing it removes every account it created.
/// </summary>
public interface IIdentityProviderDriver : IAsyncDisposable
{
    /// <summary>Creates a new customer sign-in account with this name and email, and the customer role.</summary>
    /// <param name="email">null for a unique address derived from the generated username</param>
    Task<CustomerAccount> CreateCustomerAsync(string firstName, string lastName, string? email);
}
