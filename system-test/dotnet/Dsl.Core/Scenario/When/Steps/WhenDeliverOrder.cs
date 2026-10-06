using Dsl.Core.Scenario.When.Steps.Base;
using Dsl.Port.When.Steps;
using Dsl.Core.Shared;
using Common;
using Driver.Adapter;
using Optivem.Testing;
using static Dsl.Core.Scenario.ScenarioDefaults;
using Dsl.Port;

namespace Dsl.Core.Scenario.When.Steps;

public class DeliverOrder : BaseWhen<VoidValue, VoidVerification>, IWhenDeliverOrder
{
    private string? _orderNumber;

    public DeliverOrder(UseCaseDsl app, ScenarioDsl scenario, Func<Task> ensureGiven) : base(app, scenario, ensureGiven)
    {
        WithOrderNumber(DefaultOrderNumber);
    }

    public DeliverOrder WithOrderNumber(string? orderNumber)
    {
        _orderNumber = orderNumber;
        return this;
    }

    IWhenDeliverOrder IWhenDeliverOrder.WithOrderNumber(string? orderNumber) => WithOrderNumber(orderNumber);

    protected override async Task<ExecutionResult<VoidValue, VoidVerification>> Execute(UseCaseDsl app)
    {
        var shop = await app.MyShop(ChannelMode.Dynamic, Channel);
        var result = await shop.DeliverOrder()
            .OrderNumber(_orderNumber)
            .Execute();

        return new ExecutionResultBuilder<VoidValue, VoidVerification>(result)
            .OrderNumber(_orderNumber)
            .Build();
    }
}
