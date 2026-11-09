using System.ComponentModel.DataAnnotations;

namespace MyCompany.MyShop.Backend.Core.Dtos.Validation;

/// <summary>
/// A non-empty value must be exactly one of the countries the shop ships to. An empty value passes:
/// [Required] reports it, so a missing country gets one error, not two.
/// </summary>
[AttributeUsage(AttributeTargets.Property)]
public sealed class SupportedCountryAttribute : ValidationAttribute
{
    public static readonly IReadOnlySet<string> SupportedCountries = new HashSet<string> { "US", "DE", "GB" };

    public override bool IsValid(object? value)
    {
        var country = (value as string)?.Trim();
        return string.IsNullOrEmpty(country) || SupportedCountries.Contains(country);
    }
}
