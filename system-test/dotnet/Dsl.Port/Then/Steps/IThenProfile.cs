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

    /// <summary>The email is shown but cannot be edited: it comes from the sign-in account (UI only).</summary>
    IThenProfile HasEmailThatCannotBeChanged();

    /// <summary>The country select offers exactly these countries, in this order (UI only).</summary>
    IThenProfile HasCountryChoices(params string[] expectedCountries);

    /// <summary>The profile's note on how sales tax is charged (UI only).</summary>
    IThenProfile HasTaxNotice(string expectedNotice);

    /// <summary>The customer was told this after saving (UI only).</summary>
    IThenProfile HasMessage(string expectedMessage);

    TaskAwaiter GetAwaiter();
}
