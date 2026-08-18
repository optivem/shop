using Common.Domain;

namespace Driver.Port.Dtos;

public class ViewOrderResponse
{
    public required string OrderNumber { get; set; }
    public required DateTime OrderTimestamp { get; set; }
    public required string Sku { get; set; }
    public required int Quantity { get; set; }
    public required decimal UnitPrice { get; set; }
    public required decimal BasePrice { get; set; }
    public required decimal DiscountRate { get; set; }
    public required decimal DiscountAmount { get; set; }
    public required decimal SubtotalPrice { get; set; }
    public required decimal TaxRate { get; set; }
    public required decimal TaxAmount { get; set; }
    public required decimal TotalPrice { get; set; }
    public required OrderStatus Status { get; set; }
    public required string Country { get; set; }
    public string? AppliedCouponCode { get; set; }
}
