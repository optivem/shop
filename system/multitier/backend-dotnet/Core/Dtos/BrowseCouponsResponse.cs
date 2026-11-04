using System.Text.Json.Serialization;
using MyCompany.MyShop.Backend.Core.Dtos.Json;

namespace MyCompany.MyShop.Backend.Core.Dtos;

public class BrowseCouponsResponse
{
    public List<BrowseCouponsItemResponse> Coupons { get; set; } = new();

    /// <summary>The 1-based page that was served, which is the default when the request left it out.</summary>
    public int Page { get; set; }

    /// <summary>The page size that was served, which is the default when the request left it out.</summary>
    public int Size { get; set; }

    public long TotalElements { get; set; }

    public int TotalPages { get; set; }
}

public class BrowseCouponsItemResponse
{
    public string Code { get; set; } = null!;
    [JsonConverter(typeof(RateJsonConverter))]
    public decimal DiscountRate { get; set; }
    public DateTime? ValidFrom { get; set; }
    public DateTime? ValidTo { get; set; }
    public int? UsageLimit { get; set; }
    public int UsedCount { get; set; }
}
