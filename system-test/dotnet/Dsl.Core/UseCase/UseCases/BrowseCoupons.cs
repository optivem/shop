using Driver.Port;
using Driver.Port.Dtos;
using Driver.Port.Dtos.Error;
using Dsl.Core.Shared;
using Dsl.Core.UseCase.UseCases.Base;

namespace Dsl.Core.UseCase.UseCases;

public class BrowseCoupons : BaseMyShopUseCase<BrowseCouponsResponse, BrowseCouponsVerification>
{
    private string? _page;
    private string? _size;

    public BrowseCoupons(IMyShopDriver driver, UseCaseContext context)
        : base(driver, context)
    {
    }

    public BrowseCoupons Page(string? page)
    {
        _page = page;
        return this;
    }

    public BrowseCoupons Size(string? size)
    {
        _size = size;
        return this;
    }

    public override async Task<MyShopUseCaseResult<BrowseCouponsResponse, BrowseCouponsVerification>> Execute()
    {
        var result = await _driver.BrowseCouponsAsync(new BrowseCouponsRequest { Page = _page, Size = _size });

        return new MyShopUseCaseResult<BrowseCouponsResponse, BrowseCouponsVerification>(
            result,
            _context,
            (response, ctx) => new BrowseCouponsVerification(response, ctx));
    }
}
