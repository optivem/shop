using System.ComponentModel.DataAnnotations;
using MyCompany.MyShop.Monolith.Core.Dtos.Validation;

namespace MyCompany.MyShop.Monolith.Core.Dtos;

/// <summary>The caller's name and default shipping address. Null, empty and whitespace-only fields are rejected.</summary>
public class UpdateProfileRequest
{
    [Required(ErrorMessage = "First name is required")]
    public string? FirstName { get; set; }

    [Required(ErrorMessage = "Last name is required")]
    public string? LastName { get; set; }

    [Required(ErrorMessage = "Street is required")]
    public string? Street { get; set; }

    [Required(ErrorMessage = "Postal code is required")]
    public string? PostalCode { get; set; }

    [Required(ErrorMessage = "City is required")]
    public string? City { get; set; }

    [Required(ErrorMessage = "Country is required")]
    [SupportedCountry(ErrorMessage = "Country must be US, DE or GB")]
    public string? Country { get; set; }
}
