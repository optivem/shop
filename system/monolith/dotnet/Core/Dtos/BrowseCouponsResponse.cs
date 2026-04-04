namespace Optivem.Shop.Monolith.Core.Dtos;

public class BrowseCouponsResponse
{
    public List<BrowseCouponsItemResponse> Coupons { get; set; } = new();
}

public class BrowseCouponsItemResponse
{
    public string Code { get; set; } = null!;
    public decimal DiscountRate { get; set; }
    public DateTime? ValidFrom { get; set; }
    public DateTime? ValidTo { get; set; }
    public int? UsageLimit { get; set; }
    public int UsedCount { get; set; }
}
