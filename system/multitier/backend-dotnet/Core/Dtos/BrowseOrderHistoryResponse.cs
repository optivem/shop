using Optivem.Shop.Backend.Core.Entities;

namespace Optivem.Shop.Backend.Core.Dtos;

public class BrowseOrderHistoryResponse
{
    public List<BrowseOrderHistoryItemResponse> Orders { get; set; } = new();
}

public class BrowseOrderHistoryItemResponse
{
    public string OrderNumber { get; set; } = null!;
    public DateTime OrderTimestamp { get; set; }
    public string Sku { get; set; } = null!;
    public string Country { get; set; } = null!;
    public int Quantity { get; set; }
    public decimal TotalPrice { get; set; }
    public OrderStatus Status { get; set; }
    public string? AppliedCouponCode { get; set; }
}
