using Dsl.Port.Then.Steps;
using Dsl.Core.Shared;
using Dsl.Core.Scenario;
using Dsl.Core.Shop.UseCases;

namespace Dsl.Core.Scenario.Then;

public class ThenSuccessOrder<TSuccessResponse, TSuccessVerification>
    : BaseThenResultOrder<TSuccessResponse, TSuccessVerification, ThenSuccessOrder<TSuccessResponse, TSuccessVerification>>, IThenOrder
    where TSuccessVerification : ResponseVerification<TSuccessResponse>
{
    internal ThenSuccessOrder(
        ThenStage<TSuccessResponse, TSuccessVerification> thenClause,
        Func<Task<string>> orderNumberFactory)
        : base(thenClause, orderNumberFactory)
    {
    }

    protected override Task<ViewOrderVerification?> RunPrelude(ExecutionResult<TSuccessResponse, TSuccessVerification> result)
    {
        var verification = result.Result.ShouldSucceed();
        return Task.FromResult(verification as ViewOrderVerification);
    }
}

