using Dsl.Port.Given.Steps.Base;

namespace Dsl.Port.Given.Steps;

/// <summary>A saved profile; unset fields keep their defaults (Ana Novak, Main St 1, 10115 Berlin, DE).</summary>
public interface IGivenProfile : IGivenStep
{
    IGivenProfile WithFirstName(string? firstName);

    IGivenProfile WithLastName(string? lastName);

    IGivenProfile WithStreet(string? street);

    IGivenProfile WithPostalCode(string? postalCode);

    IGivenProfile WithCity(string? city);

    IGivenProfile WithCountry(string? country);

    /// <summary>The profile is saved by the default customer (customer1).</summary>
    IGivenProfile SavedByCustomer();

    /// <summary>The profile is saved by the customer known in this scenario by the given alias.</summary>
    IGivenProfile SavedByCustomer(string alias);
}
