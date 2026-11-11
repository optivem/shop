namespace Driver.Port.Dtos;

/// <summary>
/// The product page as shown. The API channel fills only the product fields; everything else is UI-only and stays
/// null there.
/// </summary>
public class ViewProductResponse
{
    public string? Sku { get; set; }
    public string? Title { get; set; }
    public string? Brand { get; set; }
    public string? Category { get; set; }
    public string? Description { get; set; }

    /// <summary>The price as the channel shows it: "$1,299.99" on the UI, the plain amount on the API.</summary>
    public string? Price { get; set; }

    public List<string>? Breadcrumbs { get; set; }

    /// <summary>The quantity field's value.</summary>
    public string? Quantity { get; set; }

    /// <summary>The notification, the "Product not found" heading or the ERP outage message, whichever shows.</summary>
    public string? Message { get; set; }

    public bool? BackToCatalogOffered { get; set; }
    public bool? AddToCartOffered { get; set; }

    /// <summary>The header's cart badge; 0 when it is not shown.</summary>
    public int? CartItemCount { get; set; }

    /// <summary>The SKU of the product page the visitor is on, from the page address; null on any other page.</summary>
    public string? PageSku { get; set; }

    /// <summary>The cart's lines, read only after adding to the cart.</summary>
    public List<CartLineDto>? CartLines { get; set; }

    /// <summary>The catalog's products, read only after following the link back to the catalog.</summary>
    public List<string>? CatalogSkus { get; set; }

    public record CartLineDto(string Sku, int Quantity);
}
