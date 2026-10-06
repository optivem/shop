using Common.Domain;

namespace Driver.Port.Dtos;

public class BrowseOrderHistoryResponse
{
    public List<OrderDto>? Orders { get; set; }
    public int Page { get; set; }
    public int Size { get; set; }
    public long TotalElements { get; set; }
    public int TotalPages { get; set; }

    public class OrderDto
    {
        public string? OrderNumber { get; set; }
        public DateTime OrderTimestamp { get; set; }
        public string? Sku { get; set; }
        public string? Country { get; set; }
        public int Quantity { get; set; }
        public decimal TotalPrice { get; set; }
        public OrderStatus Status { get; set; }
        public string? AppliedCouponCode { get; set; }
        public string? Customer { get; set; }
    }
}
