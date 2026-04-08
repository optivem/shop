using Dsl.Port.Then.Steps;
using Dsl.Core.Shared;
using Dsl.Core.Scenario;
using Dsl.Core.Shop.UseCases;
using Dsl.Core.Shop.UseCases.Base;

namespace Dsl.Core.Scenario.Then;

/// <summary>
/// Order verification in failure path - no success check, runs failure assertions first then order verifications.
/// </summary>
public class ThenFailureOrder<TSuccessResponse, TSuccessVerification>
    : BaseThenResultOrder<TSuccessResponse, TSuccessVerification, ThenFailureOrder<TSuccessResponse, TSuccessVerification>>, IThenOrder
    where TSuccessVerification : ResponseVerification<TSuccessResponse>
{
    private readonly List<Action<SystemErrorFailureVerification>> _failureAssertions;

    internal ThenFailureOrder(
        ThenStage<TSuccessResponse, TSuccessVerification> thenClause,
        List<Action<SystemErrorFailureVerification>> failureAssertions,
        Func<Task<string>> orderNumberFactory)
        : base(thenClause, orderNumberFactory)
    {
        _failureAssertions = failureAssertions;
    }

    protected override Task<ViewOrderVerification?> RunPrelude(ExecutionResult<TSuccessResponse, TSuccessVerification> result)
    {
        if (result.Result == null)
            throw new InvalidOperationException("Cannot verify failure: no operation was executed");
        var failureVerification = result.Result.ShouldFail();
        foreach (var assertion in _failureAssertions)
        {
            assertion(failureVerification);
        }
        return Task.FromResult<ViewOrderVerification?>(null);
    }
}


