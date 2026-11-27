using Common;
using Driver.Port.External.Identity.Dtos.Error;

namespace Driver.Port.External.Identity;

/// <summary>
/// The identity provider's test realm, for scenarios that need a user of their own: a customer (no saved profile, no
/// orders) or an admin. Apart from <see cref="GoToIdentityProviderAsync"/> it arranges, never asserts, so a failure is
/// thrown rather than returned. Disposing it removes every account it created.
/// </summary>
public interface IIdentityProviderDriver : IAsyncDisposable
{
    /// <summary>Whether the test realm is served, without signing in; the smoke tests' check.</summary>
    Task<Result<VoidValue, IdentityProviderErrorResponse>> GoToIdentityProviderAsync();

    /// <summary>Creates a new customer sign-in account with this name and email, and the customer role.</summary>
    /// <param name="email">null for a unique address derived from the generated username</param>
    Task<UserAccount> CreateCustomerAsync(string firstName, string lastName, string? email);

    /// <summary>Creates a new admin sign-in account with this name and email, and the admin role.</summary>
    /// <param name="email">null for a unique address derived from the generated username</param>
    Task<UserAccount> CreateAdminAsync(string firstName, string lastName, string? email);

    /// <summary>
    /// A new unique username and password for a customer account that is created elsewhere (a visitor's sign-up,
    /// UC-013); call <see cref="DeleteOnDisposeAsync"/> once it exists.
    /// </summary>
    UserAccount NewAccountCredentials();

    /// <summary>A unique email for an account from <see cref="NewAccountCredentials"/>.</summary>
    string NewAccountEmail(UserAccount account);

    /// <summary>Removes the account with this username on dispose, like the ones this driver created.</summary>
    Task DeleteOnDisposeAsync(string username);
}
