using Dsl.Core.Scenario.Then.Steps;
using Dsl.Port.Then;
using Dsl.Port.Then.Steps;
using Optivem.Testing;

namespace Dsl.Core.Scenario.Then;

public class ThenStageBase : IThenStage
{
    protected readonly UseCaseDsl _app;
    private readonly Func<Task>? _setup;
    private bool _setupCompleted;

    public ThenStageBase(UseCaseDsl app, Func<Task>? setup = null)
    {
        _app = app;
        _setup = setup;
        _setupCompleted = false;
    }

    public IThenClock Clock()
    {
        return new Steps.ThenClock(_app, EnsureSetup);
    }

    public IThenProduct Product(string skuAlias)
    {
        return new Steps.ThenProduct(_app, skuAlias, EnsureSetup);
    }

    public IThenCountry Country(string countryAlias)
    {
        return new Steps.ThenCountry(_app, countryAlias, EnsureSetup);
    }

    protected async Task EnsureSetup()
    {
        if (!_setupCompleted && _setup != null)
        {
            await _setup();
            _setupCompleted = true;
        }
    }
}
