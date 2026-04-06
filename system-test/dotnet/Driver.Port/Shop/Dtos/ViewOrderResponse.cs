namespace Driver.Port.Shop.Dtos;

public class ViewOrderResponse
{
    public required string OrderNumber { get; set; }
    public required DateTime OrderTimestamp { get; set; }
    public required string Sku { get; set; }
    public required int Quantity { get; set; }
    public required decimal UnitPrice { get; set; }
    public decimal? SubtotalPrice { get; set; }
    public decimal? TaxRate { get; set; }
    public decimal? DiscountRate { get; set; }
    public string? AppliedCouponCode { get; set; }
    public required decimal TotalPrice { get; set; }
    public required OrderStatus Status { get; set; }
}

