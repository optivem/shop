using Dsl.Port.Given.Steps.Base;

namespace Dsl.Port.Given.Steps;

/// <summary>
/// A customer of the scenario's own: a new sign-in account with no saved profile and no orders. It becomes the
/// scenario's customer, so <c>LoggedInAsCustomer()</c>, <c>Profile().SavedByCustomer()</c> and
/// <c>Order().PlacedByCustomer()</c> all mean this customer. Unset names keep the defaults (Ana Novak); the email
/// is unique unless set.
/// </summary>
public interface IGivenNewCustomer : IGivenStep
{
    /// <summary>The sign-in account's first name.</summary>
    IGivenNewCustomer WithFirstName(string firstName);

    /// <summary>The sign-in account's last name.</summary>
    IGivenNewCustomer WithLastName(string lastName);

    /// <summary>The sign-in account's email.</summary>
    IGivenNewCustomer WithEmail(string email);
}
