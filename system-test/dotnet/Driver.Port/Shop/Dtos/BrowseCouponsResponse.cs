namespace Driver.Port.Shop.Dtos;

public class BrowseCouponsResponse
{
    public required List<BrowseCouponItemResponse> Coupons { get; set; }
}

public class BrowseCouponItemResponse
{
    public required string Code { get; set; }
    public required decimal DiscountRate { get; set; }
}
