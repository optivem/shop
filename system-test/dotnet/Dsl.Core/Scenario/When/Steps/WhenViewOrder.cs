using Driver.Adapter;
using Dsl.Core.Scenario.When.Steps.Base;
using Dsl.Port;
using Dsl.Port.When.Steps;
using Driver.Port.Shop.Dtos;
using Dsl.Core.Shop.UseCases;
using Optivem.Testing;
using static Dsl.Core.Gherkin.GherkinDefaults;

namespace Dsl.Core.Scenario.When.Steps;

public class ViewOrder : BaseWhen<ViewOrderResponse, ViewOrderVerification>, IViewOrder
{
    private string? _orderNumber;

    public ViewOrder(UseCaseDsl app, ScenarioDsl scenario, Func<Task> ensureGiven) : base(app, scenario, ensureGiven)
    {
        WithOrderNumber(DefaultOrderNumber);
    }

    public ViewOrder WithOrderNumber(string? orderNumber)
    {
        _orderNumber = orderNumber;
        return this;
    }

    IViewOrder IViewOrder.WithOrderNumber(string? orderNumber) => WithOrderNumber(orderNumber);

    protected override async Task<ExecutionResult<ViewOrderResponse, ViewOrderVerification>> Execute(UseCaseDsl app)
    {
        var shop = await app.Shop(ChannelMode.Dynamic, Channel);
        var result = await shop.ViewOrder()
            .OrderNumber(_orderNumber)
            .Execute();

        return new ExecutionResultBuilder<ViewOrderResponse, ViewOrderVerification>(result)
            .OrderNumber(_orderNumber)
            .Build();
    }
}



