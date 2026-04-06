namespace Driver.Port.Shop.Dtos;

public class PlaceOrderRequest
{
    public string? Sku { get; set; }
    public string? Quantity { get; set; }
    public string? Country { get; set; }
    public string? CouponCode { get; set; }
}

