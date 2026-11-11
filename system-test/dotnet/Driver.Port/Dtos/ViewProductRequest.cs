namespace Driver.Port.Dtos;

/// <summary>
/// How the visitor views a product, applied in this order: put the cart line in the cart, open the product (by its
/// page address, or from the catalog), type a quantity, use the stepper, add to the cart; then the follow-up, if any.
/// Unset inputs are left untouched. Only the SKU reaches the API: the rest are UI controls.
/// </summary>
public class ViewProductRequest
{
    /// <summary>Raises the quantity by 1 with the stepper.</summary>
    public const string Increase = "increase";

    /// <summary>Lowers the quantity by 1 with the stepper.</summary>
    public const string Decrease = "decrease";

    /// <summary>Follows the link back to the catalog.</summary>
    public const string BackToCatalog = "back-to-catalog";

    public string? Sku { get; set; }

    /// <summary>Opens the product from its card in the catalog instead of by its page address.</summary>
    public bool? FromCatalog { get; set; }

    /// <summary>A product already in the cart before the visitor views this one, with <see cref="CartQuantity"/>.</summary>
    public string? CartSku { get; set; }
    public string? CartQuantity { get; set; }

    /// <summary>The quantity typed into the quantity field.</summary>
    public string? Quantity { get; set; }

    /// <summary><see cref="Increase"/> or <see cref="Decrease"/>.</summary>
    public string? QuantityStep { get; set; }

    /// <summary>Adds the product to the cart with the quantity in the field.</summary>
    public bool? AddToCart { get; set; }

    /// <summary><see cref="BackToCatalog"/>, run after the other inputs.</summary>
    public string? FollowUp { get; set; }
}
