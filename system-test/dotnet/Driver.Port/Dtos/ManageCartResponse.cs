namespace Driver.Port.Dtos;

/// <summary>
/// The site after the visitor's action: the notification and cart badge where the action ended, then the cart as
/// the cart page shows it.
/// </summary>
public class ManageCartResponse
{
    /// <summary>The action's notification; else the cart page's empty-cart or ERP-outage message; else null.</summary>
    public string? Message { get; set; }

    /// <summary>The header's cart badge where the action ended; 0 when it is not shown.</summary>
    public int? CartItemCount { get; set; }

    /// <summary>The cart's lines, in the order shown.</summary>
    public List<CartLineDto>? Lines { get; set; }

    /// <summary>The "(n items)" count next to the heading; null when it is not shown.</summary>
    public int? ItemCount { get; set; }

    public string? Subtotal { get; set; }

    /// <summary>The note that tax and discounts are calculated at checkout; null when it is not shown.</summary>
    public string? CheckoutNote { get; set; }

    public bool? StartShoppingOffered { get; set; }

    /// <summary>Whether the visitor ended up on the login form.</summary>
    public bool? AskedToSignIn { get; set; }

    public record CartLineDto(string Sku, string Title, string UnitPrice, int Quantity, string LineTotal);
}
