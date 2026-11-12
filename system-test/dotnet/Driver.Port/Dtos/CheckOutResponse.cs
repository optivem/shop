namespace Driver.Port.Dtos;

/// <summary>
/// Where the checkout left the customer: the checkout as it was opened and as it ended, the order confirmation, or the
/// cart; and, once the order was placed, the cart.
/// </summary>
public class CheckOutResponse
{
    public const string Checkout = "checkout";
    public const string Confirmation = "confirmation";
    public const string Cart = "cart";

    /// <summary><see cref="Checkout"/>, <see cref="Confirmation"/> or <see cref="Cart"/>.</summary>
    public string? ShownPage { get; set; }

    /// <summary>The error notification's message on the checkout, or the cart's message; else null.</summary>
    public string? Message { get; set; }

    /// <summary>The error notification's lines under its message, as shown.</summary>
    public List<string>? MessageDetails { get; set; }

    // The checkout
    public List<ReviewLineDto>? ReviewLines { get; set; }

    public string? ReviewSubtotal { get; set; }

    /// <summary>The tax country and estimated tax rate when the checkout was opened, before any change.</summary>
    public string? OpenedTaxCountry { get; set; }

    public string? OpenedEstimatedTaxRate { get; set; }

    public string? TaxCountry { get; set; }

    public string? EstimatedTaxRate { get; set; }

    public string? ReviewNote { get; set; }

    public bool? SavedAddressSelected { get; set; }

    /// <summary>The saved address as the checkout shows it; null when there is none.</summary>
    public string? SavedAddress { get; set; }

    /// <summary>The shipping address form is shown (always on a first order; on choosing another address otherwise).</summary>
    public bool? AddressFormOpen { get; set; }

    /// <summary>The address form's first and last name as shown; null when the form is not shown.</summary>
    public string? ShippingFirstName { get; set; }

    public string? ShippingLastName { get; set; }

    /// <summary>"Save as my default shipping address" is offered and ticked.</summary>
    public bool? SaveAsDefaultTicked { get; set; }

    // The confirmation
    /// <summary>The confirmation's heading; null when it thanks for no order.</summary>
    public string? Thanks { get; set; }

    /// <summary>The confirmed order numbers, in the order shown.</summary>
    public List<ConfirmedOrderDto>? ConfirmedOrders { get; set; }

    public List<RejectedLineDto>? RejectedLines { get; set; }

    public bool? ViewMyOrdersOffered { get; set; }

    public bool? ContinueShoppingOffered { get; set; }

    /// <summary>The cart after the order was placed (or rejected); null when no order was placed.</summary>
    public List<ViewProductResponse.CartLineDto>? CartLines { get; set; }

    public record ReviewLineDto(string Sku, int Quantity, string LineTotal);

    /// <param name="Link">Where the order number links to.</param>
    public record ConfirmedOrderDto(string OrderNumber, string? Link);

    public record RejectedLineDto(string Sku, string Reason);
}
