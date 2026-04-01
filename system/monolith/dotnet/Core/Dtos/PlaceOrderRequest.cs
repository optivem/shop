using System.ComponentModel.DataAnnotations;

namespace Optivem.Starter.Monolith.Core.Dtos;

public class PlaceOrderRequest
{
    [Required(ErrorMessage = "SKU must not be empty")]
    public string? Sku { get; set; }

    [Required(ErrorMessage = "Quantity must not be empty")]
    [Range(1, int.MaxValue, ErrorMessage = "Quantity must be positive")]
    public int? Quantity { get; set; }
}
