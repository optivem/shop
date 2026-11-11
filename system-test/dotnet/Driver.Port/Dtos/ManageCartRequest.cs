namespace Driver.Port.Dtos;

/// <summary>
/// How the visitor manages the cart: a fresh browser session first gets the cart lines, each added from its product
/// page in the order given; then the action runs. With <see cref="ContinueSession"/> the previous call's browser
/// session is kept instead, so only the action runs. Without an action, the call only fills the cart.
/// </summary>
public class ManageCartRequest
{
    /// <summary>Opens the cart.</summary>
    public const string ViewCart = "view-cart";

    /// <summary>Opens the catalog.</summary>
    public const string BrowseCatalog = "browse-catalog";

    /// <summary>Adds <see cref="Sku"/> with its card's "Add to cart" in the catalog.</summary>
    public const string AddFromCatalog = "add-from-catalog";

    /// <summary>Adds <see cref="Quantity"/> of <see cref="Sku"/> from its product page.</summary>
    public const string AddFromProductPage = "add-from-product-page";

    /// <summary>Types <see cref="Quantity"/> into the cart line of <see cref="Sku"/>.</summary>
    public const string ChangeQuantity = "change-quantity";

    /// <summary>Raises the cart line of <see cref="Sku"/> by 1 with the stepper.</summary>
    public const string IncreaseQuantity = "increase-quantity";

    /// <summary>Lowers the cart line of <see cref="Sku"/> by 1 with the stepper.</summary>
    public const string DecreaseQuantity = "decrease-quantity";

    /// <summary>Removes the cart line of <see cref="Sku"/>.</summary>
    public const string RemoveLine = "remove-line";

    /// <summary>Leaves the site and comes back in the same browser.</summary>
    public const string Revisit = "revisit";

    /// <summary>Signs in as the customer from the header.</summary>
    public const string SignIn = "sign-in";

    public List<CartLineDto>? CartLines { get; set; }

    public bool? ContinueSession { get; set; }

    public string? Action { get; set; }

    public string? Sku { get; set; }

    public string? Quantity { get; set; }

    public class CartLineDto
    {
        public string? Sku { get; set; }
        public string? Quantity { get; set; }
    }
}
