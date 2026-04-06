using Driver.Port.Shop;
using Driver.Port.Shop.Dtos;
using Driver.Port.Shop.Dtos.Error;
using Dsl.Core.Shared;
using Dsl.Core.Shop.UseCases.Base;

namespace Dsl.Core.Shop.UseCases;

public class BrowseCoupons : BaseShopCommand<BrowseCouponsResponse, BrowseCouponsVerification>
{
    public BrowseCoupons(IShopDriver driver, UseCaseContext context)
        : base(driver, context)
    {
    }

    public override async Task<ShopUseCaseResult<BrowseCouponsResponse, BrowseCouponsVerification>> Execute()
    {
        var result = await _driver.BrowseCouponsAsync();

        return new ShopUseCaseResult<BrowseCouponsResponse, BrowseCouponsVerification>(
            result,
            _context,
            (response, ctx) => new BrowseCouponsVerification(response, ctx));
    }
}
