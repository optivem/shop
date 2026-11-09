using System.ComponentModel.DataAnnotations;

namespace MyCompany.MyShop.Backend.Core.Dtos;

/// <summary>
/// The address an order is shipped to (and the tax country it is charged in). Null, empty and
/// whitespace-only fields are rejected; the stored and returned values are trimmed.
/// </summary>
public class ShippingAddress
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
    public string? Country { get; set; }

    public ShippingAddress Trimmed() => new()
    {
        FirstName = FirstName?.Trim(),
        LastName = LastName?.Trim(),
        Street = Street?.Trim(),
        PostalCode = PostalCode?.Trim(),
        City = City?.Trim(),
        Country = Country?.Trim()
    };
}
