using Common;
using Driver.Port.Shop;
using Driver.Port.Shop.Dtos;
using Driver.Port.Shop.Dtos.Error;
using Dsl.Core.Shared;
using Dsl.Core.Shop.UseCases.Base;

namespace Dsl.Core.Shop.UseCases;

public class PublishCoupon : BaseShopCommand<VoidValue, VoidVerification>
{
    private string? _code;
    private decimal? _discountRate;

    public PublishCoupon(IShopDriver driver, UseCaseContext context)
        : base(driver, context)
    {
    }

    public PublishCoupon Code(string? code)
    {
        _code = code;
        return this;
    }

    public PublishCoupon DiscountRate(decimal? discountRate)
    {
        _discountRate = discountRate;
        return this;
    }

    public override async Task<ShopUseCaseResult<VoidValue, VoidVerification>> Execute()
    {
        var request = new PublishCouponRequest
        {
            Code = _code,
            DiscountRate = _discountRate,
        };

        var result = await _driver.PublishCouponAsync(request);

        return new ShopUseCaseResult<VoidValue, VoidVerification>(
            result,
            _context,
            (response, ctx) => new VoidVerification(response, ctx));
    }
}
