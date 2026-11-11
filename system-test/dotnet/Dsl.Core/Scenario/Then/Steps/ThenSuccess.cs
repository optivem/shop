using System.Runtime.CompilerServices;
using Dsl.Port.Then.Steps;
using Dsl.Core.Shared;

namespace Dsl.Core.Scenario.Then;

/// <summary>
/// Deferred success assertion builder - allows chaining .And().Order().HasStatus(...) before awaiting.
/// Enables fluent syntax: await Scenario(...).Then().ShouldSucceed().And().Order().HasStatus(...);
/// </summary>
public class ThenSuccess<TSuccessResponse, TSuccessVerification>
    : IThenSuccess
    where TSuccessVerification : ResponseVerification<TSuccessResponse>
{
    private readonly ThenStage<TSuccessResponse, TSuccessVerification> _thenClause;

    internal ThenSuccess(ThenStage<TSuccessResponse, TSuccessVerification> thenClause)
    {
        _thenClause = thenClause;
    }

    public ThenSuccessAnd<TSuccessResponse, TSuccessVerification> And()
    {
        return new ThenSuccessAnd<TSuccessResponse, TSuccessVerification>(_thenClause);
    }

    IThenSuccessAnd IThenSuccess.And() => And();

    public ThenSuccessOrderHistory<TSuccessResponse, TSuccessVerification> OrderHistory()
    {
        return new ThenSuccessOrderHistory<TSuccessResponse, TSuccessVerification>(_thenClause);
    }

    IThenOrderHistory IThenSuccess.OrderHistory() => OrderHistory();

    public ThenSuccessCatalog<TSuccessResponse, TSuccessVerification> Catalog()
    {
        return new ThenSuccessCatalog<TSuccessResponse, TSuccessVerification>(_thenClause);
    }

    IThenCatalog IThenSuccess.Catalog() => Catalog();

    public ThenSuccessProductPage<TSuccessResponse, TSuccessVerification> ProductPage()
    {
        return new ThenSuccessProductPage<TSuccessResponse, TSuccessVerification>(_thenClause);
    }

    IThenProductPage IThenSuccess.ProductPage() => ProductPage();

    public ThenSuccessCart<TSuccessResponse, TSuccessVerification> Cart()
    {
        return new ThenSuccessCart<TSuccessResponse, TSuccessVerification>(_thenClause);
    }

    IThenCart IThenSuccess.Cart() => Cart();

    /// <summary>Verifies the profile the executed operation (view or update profile) answered with.</summary>
    public IThenProfile Profile()
    {
        return new Steps.ThenProfile(_thenClause.App, () => Task.CompletedTask, async () =>
        {
            var result = await _thenClause.GetExecutionResult();
            return result.Result.ShouldSucceed() as Dsl.Core.UseCase.UseCases.ViewProfileVerification
                ?? throw new InvalidOperationException("Cannot verify profile: the executed operation did not answer with a profile");
        });
    }

    /// <summary>
    /// When awaited with no further chaining, runs the success verification.
    /// </summary>
    public TaskAwaiter GetAwaiter() => ExecuteSuccessOnly().GetAwaiter();

    private async Task ExecuteSuccessOnly()
    {
        var result = await _thenClause.GetExecutionResult();
        _ = result.Result.ShouldSucceed();
    }
}

