using System.Text.Json.Serialization;
using MyCompany.MyShop.Backend.Core.Dtos.Json;
using MyCompany.MyShop.Backend.Core.Entities;

namespace MyCompany.MyShop.Backend.Core.Dtos;

public class ViewOrderDetailsResponse
{
    public string OrderNumber { get; set; } = null!;
    public DateTime OrderTimestamp { get; set; }
    public string Sku { get; set; } = null!;
    public int Quantity { get; set; }
    [JsonConverter(typeof(MoneyJsonConverter))]
    public decimal UnitPrice { get; set; }
    [JsonConverter(typeof(MoneyJsonConverter))]
    public decimal BasePrice { get; set; }
    [JsonConverter(typeof(RateJsonConverter))]
    public decimal DiscountRate { get; set; }
    [JsonConverter(typeof(MoneyJsonConverter))]
    public decimal DiscountAmount { get; set; }
    [JsonConverter(typeof(MoneyJsonConverter))]
    public decimal SubtotalPrice { get; set; }
    [JsonConverter(typeof(RateJsonConverter))]
    public decimal TaxRate { get; set; }
    [JsonConverter(typeof(MoneyJsonConverter))]
    public decimal TaxAmount { get; set; }
    [JsonConverter(typeof(MoneyJsonConverter))]
    public decimal TotalPrice { get; set; }
    public OrderStatus Status { get; set; }
    public string Country { get; set; } = null!;
    public string? AppliedCouponCode { get; set; }
}
