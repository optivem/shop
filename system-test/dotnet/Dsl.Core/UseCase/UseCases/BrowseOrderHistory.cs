using Driver.Port;
using Dsl.Core.UseCase.UseCases.Base;
using Dsl.Core.Shared;
using Driver.Port.Dtos;

namespace Dsl.Core.UseCase.UseCases;

public class BrowseOrderHistory : BaseMyShopUseCase<BrowseOrderHistoryResponse, BrowseOrderHistoryVerification>
{
    private string? _orderNumberResultAlias;
    private string? _page;
    private string? _size;
    private string? _customer;
    private string? _followUp;

    public BrowseOrderHistory(IMyShopDriver driver, UseCaseContext context)
        : base(driver, context)
    {
    }

    public BrowseOrderHistory OrderNumber(string? orderNumberResultAlias)
    {
        _orderNumberResultAlias = orderNumberResultAlias;
        return this;
    }

    public BrowseOrderHistory Page(string? page)
    {
        _page = page;
        return this;
    }

    public BrowseOrderHistory Size(string? size)
    {
        _size = size;
        return this;
    }

    /// <summary>The username of the customer whose orders an admin filters on.</summary>
    public BrowseOrderHistory Customer(string? customerUsername)
    {
        _customer = customerUsername;
        return this;
    }

    /// <summary>After browsing, the customer filter is set to All customers and the order number cleared (UI only).</summary>
    public BrowseOrderHistory ResetFiltersAfterwards()
    {
        _followUp = BrowseOrderHistoryRequest.ResetFilters;
        return this;
    }

    public override async Task<MyShopUseCaseResult<BrowseOrderHistoryResponse, BrowseOrderHistoryVerification>> Execute()
    {
        var orderNumber = _context.GetResultValue(_orderNumberResultAlias);
        var request = new BrowseOrderHistoryRequest
        {
            OrderNumber = orderNumber, Page = _page, Size = _size, Customer = _customer, FollowUp = _followUp,
        };

        var result = await _driver.BrowseOrderHistoryAsync(request);

        return new MyShopUseCaseResult<BrowseOrderHistoryResponse, BrowseOrderHistoryVerification>(
            result,
            _context,
            (response, ctx) => new BrowseOrderHistoryVerification(response, ctx));
    }
}
