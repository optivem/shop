using System.Runtime.CompilerServices;
using Dsl.Port.Then.Steps;

namespace Dsl.Core.Scenario.Then.Steps;

/// <summary>
/// Deferred external-system lookup - records assertions, then on await runs the prelude, fetches, and verifies.
/// Enables fluent syntax: await Scenario(...).Then().ShouldSucceed().And().Clock().HasTime(...);
/// </summary>
public abstract class BaseThenLookup<TVerification>
{
    protected readonly UseCaseDsl _app;
    private readonly Func<Task> _prelude;
    private readonly List<Action<TVerification>> _verifications = [];

    protected BaseThenLookup(UseCaseDsl app, Func<Task> prelude)
    {
        _app = app;
        _prelude = prelude;
    }

    protected abstract Task<TVerification> Fetch();

    protected void AddVerification(Action<TVerification> verification)
    {
        _verifications.Add(verification);
    }

    public IThenClock Clock() => new ThenClock(_app, Execute);

    public IThenProduct Product(string skuAlias) => new ThenProduct(_app, skuAlias, Execute);

    public IThenCountry Country(string countryAlias) => new ThenCountry(_app, countryAlias, Execute);

    public TaskAwaiter GetAwaiter() => Execute().GetAwaiter();

    private async Task Execute()
    {
        await _prelude();
        var verification = await Fetch();
        foreach (var v in _verifications)
        {
            v(verification);
        }
    }
}
