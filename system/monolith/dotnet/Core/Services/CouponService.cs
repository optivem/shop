using Microsoft.EntityFrameworkCore;
using Optivem.Shop.Monolith.Core.Entities;
using Optivem.Shop.Monolith.Core.Exceptions;
using Optivem.Shop.Monolith.Core.Services.External;
using Optivem.Shop.Monolith.Data;

namespace Optivem.Shop.Monolith.Core.Services;

public class CouponService
{
    private const string FieldCouponCode = "couponCode";
    private const string MsgCouponDoesNotExist = "Coupon code {0} does not exist";
    private const string MsgCouponNotYetValid = "Coupon code {0} is not yet valid";
    private const string MsgCouponExpired = "Coupon code {0} has expired";
    private const string MsgCouponUsageLimitReached = "Coupon code {0} has exceeded its usage limit";
    private const string MsgCouponCodeAlreadyExists = "Coupon code {0} already exists";

    private readonly AppDbContext _dbContext;
    private readonly ClockGateway _clockGateway;

    public CouponService(AppDbContext dbContext, ClockGateway clockGateway)
    {
        _dbContext = dbContext;
        _clockGateway = clockGateway;
    }

    public async Task<decimal> GetDiscountAsync(string? couponCode)
    {
        if (string.IsNullOrWhiteSpace(couponCode))
        {
            return 0m;
        }

        var coupon = await _dbContext.Coupons.FirstOrDefaultAsync(c => c.Code == couponCode);

        if (coupon == null)
        {
            ThrowCouponValidationException(MsgCouponDoesNotExist, couponCode);
        }

        var now = await _clockGateway.GetCurrentTimeAsync();

        if (coupon!.ValidFrom.HasValue && now < coupon.ValidFrom.Value)
        {
            ThrowCouponValidationException(MsgCouponNotYetValid, couponCode);
        }

        if (coupon.ValidTo.HasValue && now > coupon.ValidTo.Value)
        {
            ThrowCouponValidationException(MsgCouponExpired, couponCode);
        }

        if (coupon.UsageLimit.HasValue && coupon.UsedCount >= coupon.UsageLimit.Value)
        {
            ThrowCouponValidationException(MsgCouponUsageLimitReached, couponCode);
        }

        return coupon.DiscountRate;
    }

    public async Task IncrementUsageCountAsync(string couponCode)
    {
        var coupon = await _dbContext.Coupons.FirstOrDefaultAsync(c => c.Code == couponCode);
        if (coupon != null)
        {
            coupon.UsedCount++;
            await _dbContext.SaveChangesAsync();
        }
    }

    public async Task CreateCouponAsync(string couponCode, decimal discountRate, DateTime? validFrom, DateTime? validTo, int? usageLimit)
    {
        var existing = await _dbContext.Coupons.FirstOrDefaultAsync(c => c.Code == couponCode);
        if (existing != null)
        {
            ThrowCouponValidationException(MsgCouponCodeAlreadyExists, couponCode);
        }

        var limit = usageLimit ?? int.MaxValue;

        var coupon = new Coupon
        {
            Code = couponCode,
            DiscountRate = discountRate,
            ValidFrom = validFrom,
            ValidTo = validTo,
            UsageLimit = limit,
            UsedCount = 0
        };

        _dbContext.Coupons.Add(coupon);
        await _dbContext.SaveChangesAsync();
    }

    public async Task<List<Coupon>> GetAllCouponsAsync()
    {
        return await _dbContext.Coupons.ToListAsync();
    }

    private static void ThrowCouponValidationException(string messageFormat, string couponCode)
    {
        throw new ValidationException(FieldCouponCode, string.Format(messageFormat, couponCode));
    }
}
