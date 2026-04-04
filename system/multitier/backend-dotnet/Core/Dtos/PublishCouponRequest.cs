using System.ComponentModel.DataAnnotations;

namespace Optivem.Shop.Backend.Core.Dtos;

public class PublishCouponRequest
{
    [Required(ErrorMessage = "Coupon code must not be blank")]
    public string? Code { get; set; }

    [Required(ErrorMessage = "Discount rate must not be null")]
    [Range(0.0001, 1.0, ErrorMessage = "Discount rate must be greater than 0.00 and at most 1.00")]
    public decimal? DiscountRate { get; set; }

    public DateTime? ValidFrom { get; set; }

    public DateTime? ValidTo { get; set; }

    [Range(1, int.MaxValue, ErrorMessage = "Usage limit must be positive")]
    public int? UsageLimit { get; set; }
}
