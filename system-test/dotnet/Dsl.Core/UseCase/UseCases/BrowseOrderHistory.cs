using Driver.Port;
using Dsl.Core.UseCase.UseCases.Base;
using Dsl.Core.Shared;
using Driver.Port.Dtos;

namespace Dsl.Core.UseCase.UseCases;

public class BrowseOrderHistory : BaseMyShopUseCase<BrowseOrderHistoryResponse, BrowseOrderHistoryVerification>
{
    private const int OrderNumberPartStart = 4;
    private const int OrderNumberPartLength = 8;

    private string? _orderNumberResultAlias;
    private bool _orderNumberPartOnly;
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

    /// <summary>Filters by part of this order's number, in lower case: characters from its middle, unique to it.</summary>
    public BrowseOrderHistory OrderNumberPartOf(string orderNumberResultAlias)
    {
        _orderNumberResultAlias = orderNumberResultAlias;
        _orderNumberPartOnly = true;
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
        if (_orderNumberPartOnly && orderNumber != null)
        {
            orderNumber = PartOf(orderNumber);
        }
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

    /// <summary>Eight characters after the "ORD-" prefix (the start of the generated part), in lower case.</summary>
    private static string PartOf(string orderNumber)
    {
        var start = Math.Min(OrderNumberPartStart, orderNumber.Length);
        var length = Math.Min(OrderNumberPartLength, orderNumber.Length - start);
        return orderNumber.Substring(start, length).ToLowerInvariant();
    }
}
