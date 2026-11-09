using Dsl.Port.When.Steps.Base;

namespace Dsl.Port.When.Steps;

/// <summary>Saves the logged-in user's profile; unset fields keep their defaults (Ana Novak, Main St 1, 10115 Berlin, DE).</summary>
public interface IWhenUpdateProfile : IWhenStep
{
    IWhenUpdateProfile WithFirstName(string? firstName);

    IWhenUpdateProfile WithLastName(string? lastName);

    IWhenUpdateProfile WithStreet(string? street);

    IWhenUpdateProfile WithPostalCode(string? postalCode);

    IWhenUpdateProfile WithCity(string? city);

    IWhenUpdateProfile WithCountry(string? country);
}
