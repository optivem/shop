using System.ComponentModel.DataAnnotations;

namespace MyCompany.MyShop.Backend.Core.Dtos.Validation;

/// <summary>
/// The Quick-order country: required (null, empty and whitespace-only are empty) unless the order carries
/// a shipping address, whose country is then the tax country.
/// </summary>
[AttributeUsage(AttributeTargets.Property)]
public sealed class RequiredUnlessShippingAddressAttribute : ValidationAttribute
{
    protected override ValidationResult? IsValid(object? value, ValidationContext validationContext)
    {
        if (validationContext.ObjectInstance is PlaceOrderRequest { ShippingAddress: not null })
        {
            return ValidationResult.Success;
        }

        return string.IsNullOrWhiteSpace(value as string)
            ? new ValidationResult(FormatErrorMessage(validationContext.DisplayName))
            : ValidationResult.Success;
    }
}
