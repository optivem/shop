using Optivem.Starter.Monolith.Core.Entities;

namespace Optivem.Starter.Monolith.Core.Dtos;

public class BrowseOrderHistoryResponse
{
    public List<BrowseOrderHistoryItemResponse> Orders { get; set; } = new();
}

public class BrowseOrderHistoryItemResponse
{
    public string OrderNumber { get; set; } = null!;
    public DateTime OrderTimestamp { get; set; }
    public string Sku { get; set; } = null!;
    public int Quantity { get; set; }
    public decimal TotalPrice { get; set; }
    public OrderStatus Status { get; set; }
}
