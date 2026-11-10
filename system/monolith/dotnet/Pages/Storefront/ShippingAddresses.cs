using System.Globalization;
using MyCompany.MyShop.Monolith.Core.Dtos;

namespace MyCompany.MyShop.Monolith.Pages.Storefront;

/// <summary>Shipping address rules shared by checkout and the profile page (UC-009 BR-3, UC-010 BR-2/BR-3).</summary>
public static class ShippingAddresses
{
    public static readonly IReadOnlyList<string> Countries = ["US", "DE", "GB"];
    public const string CountryNotSupported = "Country must be US, DE or GB";

    /// <summary>
    /// Estimated sales tax per shipping country, for the checkout review only (the same rates as the tax system's
    /// test data). Each order is taxed by the backend when it is placed.
    /// </summary>
    public static readonly IReadOnlyDictionary<string, decimal> EstimatedTaxRates =
        new Dictionary<string, decimal> { ["US"] = 0m, ["DE"] = 0.19m, ["GB"] = 0.20m };

    /// <summary>Field name -> error message, in form order; every field is required (spaces count as empty) and the country must be supported.</summary>
    public static IReadOnlyList<KeyValuePair<string, string>> Validate(ShippingAddress address)
    {
        var errors = new List<KeyValuePair<string, string>>();
        Require(errors, "firstName", address.FirstName, "First name");
        Require(errors, "lastName", address.LastName, "Last name");
        Require(errors, "street", address.Street, "Street");
        Require(errors, "postalCode", address.PostalCode, "Postal code");
        Require(errors, "city", address.City, "City");
        Require(errors, "country", address.Country, "Country");
        if (!errors.Exists(error => error.Key == "country") && !Countries.Contains(address.Country!.Trim()))
        {
            errors.Add(new("country", CountryNotSupported));
        }

        return errors;
    }

    public static ShippingAddress AddressOf(ViewProfileResponse profile) => new()
    {
        FirstName = profile.FirstName ?? string.Empty,
        LastName = profile.LastName ?? string.Empty,
        Street = profile.Street ?? string.Empty,
        PostalCode = profile.PostalCode ?? string.Empty,
        City = profile.City ?? string.Empty,
        Country = profile.Country ?? string.Empty,
    };

    /// <summary>The profile's saved address, or null when the customer has not saved a complete one yet.</summary>
    public static ShippingAddress? SavedAddressOf(ViewProfileResponse? profile)
    {
        if (profile == null)
        {
            return null;
        }

        var address = AddressOf(profile);
        return Validate(address).Count == 0 ? address : null;
    }

    /// <summary>"Ana Novak, Main St 1, 10115 Berlin, DE".</summary>
    public static string Format(ShippingAddress address) =>
        $"{address.FirstName} {address.LastName}, {address.Street}, {address.PostalCode} {address.City}, {address.Country}";

    /// <summary>0.19 -> "19%"; "-" for a country without a known rate.</summary>
    public static string EstimatedTaxRate(string? country) =>
        country != null && EstimatedTaxRates.TryGetValue(country.Trim(), out var rate)
            ? (rate * 100).ToString("0.##", CultureInfo.InvariantCulture) + "%"
            : "-";

    public static ShippingAddress Empty() => new()
    {
        FirstName = string.Empty,
        LastName = string.Empty,
        Street = string.Empty,
        PostalCode = string.Empty,
        City = string.Empty,
        Country = string.Empty,
    };

    private static void Require(List<KeyValuePair<string, string>> errors, string field, string? value, string label)
    {
        if (string.IsNullOrWhiteSpace(value))
        {
            errors.Add(new(field, label + " is required"));
        }
    }
}
