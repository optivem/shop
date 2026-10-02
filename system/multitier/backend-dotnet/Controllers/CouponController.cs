using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using MyCompany.MyShop.Backend.Api.Security;
using MyCompany.MyShop.Backend.Core.Dtos;
using MyCompany.MyShop.Backend.Core.Services;

namespace MyCompany.MyShop.Backend.Controllers;

[ApiController]
[Authorize(Roles = Roles.Admin)]
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
