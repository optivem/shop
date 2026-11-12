using System.Runtime.CompilerServices;

namespace Dsl.Port.Then.Steps;

/// <summary>Profile assertions; the country is a literal (the profile accepts only US, DE or GB).</summary>
public interface IThenProfile
{
    IThenProfile HasFirstName(string expectedFirstName);

    IThenProfile HasLastName(string expectedLastName);

    IThenProfile HasEmail(string expectedEmail);

    IThenProfile HasStreet(string expectedStreet);

    IThenProfile HasPostalCode(string expectedPostalCode);

    IThenProfile HasCity(string expectedCity);

    IThenProfile HasCountry(string expectedCountry);

    IThenProfile DoesNotHaveStreet(string street);

    /// <summary>No shipping address is saved: the street, postal code, city and country are empty.</summary>
    IThenProfile HasNoSavedAddress();

    TaskAwaiter GetAwaiter();
}
