using Dsl.Core.Scenario.Then;
using Dsl.Port.Then;
using Dsl.Port.When.Steps.Base;
using Dsl.Core.Shared;
using Driver.Adapter;
using Optivem.Testing;

namespace Dsl.Core.Scenario.When.Steps.Base;

public abstract class BaseWhen<TSuccessResponse, TSuccessVerification>
    : IWhenStep
    where TSuccessVerification : ResponseVerification<TSuccessResponse>
{
    private readonly UseCaseDsl _app;
    private readonly ScenarioDsl _scenario;
    private readonly Func<Task> _ensureGiven;

    protected BaseWhen(UseCaseDsl app, ScenarioDsl scenario, Func<Task> ensureGiven)
    {
        _app = app;
        _scenario = scenario;
        _ensureGiven = ensureGiven;
    }

    public ThenStage<TSuccessResponse, TSuccessVerification> Then()
    {
        _scenario.MarkAsExecuted();
        return new ThenStage<TSuccessResponse, TSuccessVerification>(Channel, _app, async () =>
        {
            await _ensureGiven();
            return await Execute(_app);
        });
    }

    IThenResultStage IWhenStep.Then() => Then();

    protected abstract Task<ExecutionResult<TSuccessResponse, TSuccessVerification>> Execute(UseCaseDsl app);

    protected Channel Channel => _scenario.Channel!;
}


