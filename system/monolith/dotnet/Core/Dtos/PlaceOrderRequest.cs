using System.ComponentModel.DataAnnotations;

namespace Optivem.Shop.Monolith.Core.Dtos;

public class PlaceOrderRequest
{
    [Required(ErrorMessage = "SKU must not be empty")]
    public string? Sku { get; set; }

    [Required(ErrorMessage = "Quantity must not be empty")]
    [Range(1, int.MaxValue, ErrorMessage = "Quantity must be positive")]
    public int? Quantity { get; set; }

    [Required(ErrorMessage = "Country must not be empty")]
    public string? Country { get; set; }

    public string? CouponCode { get; set; }
}
