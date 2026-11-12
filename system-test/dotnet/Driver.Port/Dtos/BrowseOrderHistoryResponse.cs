using Common.Domain;

namespace Driver.Port.Dtos;

public class BrowseOrderHistoryResponse
{
    public List<OrderDto>? Orders { get; set; }
    public int Page { get; set; }
    public int Size { get; set; }
    public long TotalElements { get; set; }
    public int TotalPages { get; set; }

    /// <summary>UI only: the customer filter's choices as shown ("All customers", then usernames); null when there is no filter.</summary>
    public List<string>? CustomerFilterOptions { get; set; }

    /// <summary>UI only: the customer filter's chosen option as shown, such as "All customers"; null when there is no filter.</summary>
    public string? CustomerFilterSelection { get; set; }

    /// <summary>UI only: whether the list has a Customer column.</summary>
    public bool? CustomerColumnShown { get; set; }

    /// <summary>UI only: the message the list shows instead of orders, such as "No orders found".</summary>
    public string? Message { get; set; }

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

        /// <summary>UI only: where the order's "View Details" link goes.</summary>
        public string? DetailsPath { get; set; }
    }
}
