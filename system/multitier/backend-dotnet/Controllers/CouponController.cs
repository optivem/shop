using Microsoft.AspNetCore.Mvc;
using Optivem.Shop.Backend.Core.Dtos;
using Optivem.Shop.Backend.Core.Services;

namespace Optivem.Shop.Backend.Controllers;

[ApiController]
[Route("api/coupons")]
public class CouponController : ControllerBase
{
    private readonly CouponService _couponService;

    public CouponController(CouponService couponService)
    {
        _couponService = couponService;
    }

    [HttpPost]
    public async Task<IActionResult> CreateCoupon([FromBody] PublishCouponRequest request)
    {
        await _couponService.CreateCouponAsync(
            request.Code!,
            request.DiscountRate!.Value,
            request.ValidFrom,
            request.ValidTo,
            request.UsageLimit
        );
        return NoContent();
    }

    [HttpGet]
    public async Task<IActionResult> BrowseCoupons()
    {
        var coupons = await _couponService.GetAllCouponsAsync();
        var items = coupons.Select(c => new BrowseCouponsItemResponse
        {
            Code = c.Code,
            DiscountRate = c.DiscountRate,
            ValidFrom = c.ValidFrom,
            ValidTo = c.ValidTo,
            UsageLimit = c.UsageLimit,
            UsedCount = c.UsedCount
        }).ToList();

        return Ok(new BrowseCouponsResponse { Coupons = items });
    }
}
