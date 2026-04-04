using Dsl.Core.Scenario;
using Dsl.Core.Scenario.When.Steps.Base;
using Dsl.Port;
using Dsl.Port.When.Steps;
using Dsl.Core.Shared;
using Common;
using Driver.Adapter;

namespace Dsl.Core.Scenario.When.Steps;

public class GoToShop : BaseWhen<VoidValue, VoidVerification>, IGoToShop
{
    public GoToShop(UseCaseDsl app, ScenarioDsl scenario, Func<Task> ensureGiven) : base(app, scenario, ensureGiven)
    {
    }

    protected override async Task<ExecutionResult<VoidValue, VoidVerification>> Execute(UseCaseDsl app)
    {
        var shop = await app.Shop(ChannelMode.Dynamic, Channel);
        var result = await shop.GoToShop()
            .Execute();

        return new ExecutionResultBuilder<VoidValue, VoidVerification>(result)
            .Build();
    }
}


