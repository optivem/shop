using Dsl.Core.Scenario.When.Steps.Base;
using Dsl.Port.When.Steps;
using Dsl.Core.Shared;
using Common;
using Driver.Adapter;
using Driver.Port.Dtos;
using Dsl.Core.UseCase.UseCases;
using Optivem.Testing;
using static Dsl.Core.Scenario.ScenarioDefaults;
using Dsl.Port;

namespace Dsl.Core.Scenario.When.Steps;

public class CancelOrder : BaseWhen<VoidValue, VoidVerification>, IWhenCancelOrder
{
    private string? _orderNumber;

    public CancelOrder(UseCaseDsl app, ScenarioDsl scenario, Func<Task> ensureGiven) : base(app, scenario, ensureGiven)
    {
        WithOrderNumber(DefaultOrderNumber);
    }

    public CancelOrder WithOrderNumber(string? orderNumber)
    {
        _orderNumber = orderNumber;
        return this;
    }

    IWhenCancelOrder IWhenCancelOrder.WithOrderNumber(string? orderNumber) => WithOrderNumber(orderNumber);

    protected override async Task<ExecutionResult<VoidValue, VoidVerification>> Execute(UseCaseDsl app)
    {
        var shop = await app.MyShop(ChannelMode.Dynamic, Channel);
        var result = await shop.CancelOrder()
            .OrderNumber(_orderNumber)
            .Execute();

        return new ExecutionResultBuilder<VoidValue, VoidVerification>(result)
            .OrderNumber(_orderNumber)
            .Build();
    }
}
