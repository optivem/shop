using Driver.Port;
using Dsl.Core.UseCase.UseCases.Base;
using Dsl.Core.Shared;
using Driver.Port.Dtos;

namespace Dsl.Core.UseCase.UseCases;

public class BrowseOrderHistory : BaseMyShopUseCase<BrowseOrderHistoryResponse, BrowseOrderHistoryVerification>
{
    private string? _orderNumberResultAlias;

    public BrowseOrderHistory(IMyShopDriver driver, UseCaseContext context)
        : base(driver, context)
    {
    }

    public BrowseOrderHistory OrderNumber(string? orderNumberResultAlias)
    {
        _orderNumberResultAlias = orderNumberResultAlias;
        return this;
    }

    public override async Task<MyShopUseCaseResult<BrowseOrderHistoryResponse, BrowseOrderHistoryVerification>> Execute()
    {
        var orderNumber = _context.GetResultValue(_orderNumberResultAlias);
        var request = new BrowseOrderHistoryRequest { OrderNumber = orderNumber };

        var result = await _driver.BrowseOrderHistoryAsync(request);

        return new MyShopUseCaseResult<BrowseOrderHistoryResponse, BrowseOrderHistoryVerification>(
            result,
            _context,
            (response, ctx) => new BrowseOrderHistoryVerification(response, ctx));
    }
}
