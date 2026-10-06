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

    protected override async Task<ExecutionResult<BrowseOrderHistoryResponse, BrowseOrderHistoryVerification>> Execute(UseCaseDsl app)
    {
        var shop = await app.MyShop(ChannelMode.Dynamic, Channel);
        var result = await shop.BrowseOrderHistory()
            .OrderNumber(_orderNumber)
            .Execute();

        return new ExecutionResultBuilder<BrowseOrderHistoryResponse, BrowseOrderHistoryVerification>(result)
            .OrderNumber(_orderNumber)
            .Build();
    }
}
