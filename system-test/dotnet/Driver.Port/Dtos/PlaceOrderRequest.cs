using System.Text.Json.Serialization;

namespace Driver.Port.Dtos;

public class PlaceOrderRequest
{
    public string? Sku { get; set; }
    public string? Quantity { get; set; }
    public string? Country { get; set; }
    public string? CouponCode { get; set; }

    /// <summary>Sent as the <c>Idempotency-Key</c> header, never in the body; null means no header.</summary>
    [JsonIgnore]
    public string? IdempotencyKey { get; set; }
}

