namespace Driver.Port.Dtos;

/// <summary>
/// How the customer checks out: a fresh browser session first gets the cart lines, each added from its product page
/// in the order given (with <see cref="ContinueSession"/> the previous call's session is kept instead). Then the
/// checkout is opened, by its address or from the cart; with <see cref="ShippingAddress"/> the customer ships to that
/// address instead of the saved one; with <see cref="PlaceOrder"/> the order is placed.
/// </summary>
public class CheckOutRequest
{
    /// <summary>Opens the checkout by its page address.</summary>
    public const string Open = "open";

    /// <summary>Opens the cart and follows its "Proceed to checkout".</summary>
    public const string ProceedFromCart = "proceed-from-cart";

    public List<CartLineDto>? CartLines { get; set; }

    public bool? ContinueSession { get; set; }

    public string? Entry { get; set; }

    /// <summary>After proceeding to the login form, signs in as the customer there.</summary>
    public bool? SignIn { get; set; }

    /// <summary>Ship to this address instead of the saved one; the country is a code as the form offers it.</summary>
    public ShippingAddress? ShippingAddress { get; set; }

    /// <summary>False unticks "Save as my default shipping address" (offered on a first order); null leaves it as shown.</summary>
    public bool? SaveAsDefault { get; set; }

    public string? CouponCode { get; set; }

    public bool? PlaceOrder { get; set; }

    public class CartLineDto
    {
        public string? Sku { get; set; }
        public string? Quantity { get; set; }
    }
}
