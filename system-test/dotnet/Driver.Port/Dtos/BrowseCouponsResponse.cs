using System.Collections.Generic;
using System;

namespace Driver.Port.Dtos;

public class BrowseCouponsResponse
{
    public required List<CouponDto> Coupons { get; set; }
    public int Page { get; set; }
    public int Size { get; set; }
    public long TotalElements { get; set; }
    public int TotalPages { get; set; }
}

public class CouponDto
{
    public required string Code { get; set; }
    public required decimal DiscountRate { get; set; }
    public DateTime? ValidFrom { get; set; }
    public DateTime? ValidTo { get; set; }
    public int? UsageLimit { get; set; }
    public int? UsedCount { get; set; }
}
