using Dsl.Core.Scenario.When.Steps.Base;
using Dsl.Port;
using Dsl.Port.When.Steps;
using Driver.Adapter;
using Driver.Port.Dtos;
using Dsl.Core.UseCase.UseCases;
using static Dsl.Core.Scenario.ScenarioDefaults;

namespace Dsl.Core.Scenario.When.Steps;

public class WhenBrowseOrderHistory : BaseWhen<BrowseOrderHistoryResponse, BrowseOrderHistoryVerification>, IWhenBrowseOrderHistory
{
    private string? _orderNumber;
    private string? _page;
    private string? _size;

    public WhenBrowseOrderHistory(UseCaseDsl app, ScenarioDsl scenario, Func<Task> ensureGiven)
        : base(app, scenario, ensureGiven)
    {
        WithOrderNumber(DefaultOrderNumber);
    }

    public WhenBrowseOrderHistory WithOrderNumber(string? orderNumber)
    {
        _orderNumber = orderNumber;
        return this;
    }

    IWhenBrowseOrderHistory IWhenBrowseOrderHistory.WithOrderNumber(string? orderNumber) => WithOrderNumber(orderNumber);

    public WhenBrowseOrderHistory WithPage(string? page)
    {
        _page = page;
        return this;
    }

    IWhenBrowseOrderHistory IWhenBrowseOrderHistory.WithPage(string? page) => WithPage(page);

    public WhenBrowseOrderHistory WithSize(string? size)
    {
        _size = size;
        return this;
    }

    IWhenBrowseOrderHistory IWhenBrowseOrderHistory.WithSize(string? size) => WithSize(size);

    protected override async Task<ExecutionResult<BrowseOrderHistoryResponse, BrowseOrderHistoryVerification>> Execute(UseCaseDsl app)
    {
        var shop = await app.MyShop(ChannelMode.Dynamic, Channel);
        var result = await shop.BrowseOrderHistory()
            .OrderNumber(_orderNumber)
            .Page(_page)
            .Size(_size)
            .Execute();

        return new ExecutionResultBuilder<BrowseOrderHistoryResponse, BrowseOrderHistoryVerification>(result)
            .OrderNumber(_orderNumber)
            .Build();
    }
}
