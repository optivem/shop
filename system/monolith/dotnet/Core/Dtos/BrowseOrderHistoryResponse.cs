using System.Text.Json.Serialization;
using MyCompany.MyShop.Monolith.Core.Dtos.Json;
using MyCompany.MyShop.Monolith.Core.Entities;

namespace MyCompany.MyShop.Monolith.Core.Dtos;

public class BrowseOrderHistoryResponse
{
    public List<BrowseOrderHistoryItemResponse> Orders { get; set; } = new();

    /// <summary>The 1-based page that was served, which is the default when the request left it out.</summary>
    public int Page { get; set; }

    /// <summary>The page size that was served, which is the default when the request left it out.</summary>
    public int Size { get; set; }

    public long TotalElements { get; set; }

    public int TotalPages { get; set; }
}

public class BrowseOrderHistoryItemResponse
{
    public string OrderNumber { get; set; } = null!;
    public DateTime OrderTimestamp { get; set; }
    public string Sku { get; set; } = null!;
    public string Country { get; set; } = null!;
    public int Quantity { get; set; }
    [JsonConverter(typeof(MoneyJsonConverter))]
    public decimal TotalPrice { get; set; }
    public OrderStatus Status { get; set; }
    public string? AppliedCouponCode { get; set; }
    public string? Customer { get; set; }
}
