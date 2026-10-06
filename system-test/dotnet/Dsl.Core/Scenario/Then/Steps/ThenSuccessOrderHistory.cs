using System.Runtime.CompilerServices;
using Dsl.Port.Then.Steps;
using Dsl.Core.Shared;
using Dsl.Core.UseCase.UseCases;

namespace Dsl.Core.Scenario.Then;

public class ThenSuccessOrderHistory<TSuccessResponse, TSuccessVerification> : IThenOrderHistory
    where TSuccessVerification : ResponseVerification<TSuccessResponse>
{
    private readonly ThenStage<TSuccessResponse, TSuccessVerification> _thenClause;
    private readonly List<Action<BrowseOrderHistoryVerification>> _verifications = [];

    internal ThenSuccessOrderHistory(ThenStage<TSuccessResponse, TSuccessVerification> thenClause)
    {
        _thenClause = thenClause;
    }

    public ThenSuccessOrderHistory<TSuccessResponse, TSuccessVerification> ContainsOrder(string orderNumber)
    {
        _verifications.Add(v => v.ContainsOrder(orderNumber));
        return this;
    }

    IThenOrderHistory IThenOrderHistory.ContainsOrder(string orderNumber) => ContainsOrder(orderNumber);

    public ThenSuccessOrderHistory<TSuccessResponse, TSuccessVerification> DoesNotContainOrder(string orderNumber)
    {
        _verifications.Add(v => v.DoesNotContainOrder(orderNumber));
        return this;
    }

    IThenOrderHistory IThenOrderHistory.DoesNotContainOrder(string orderNumber) => DoesNotContainOrder(orderNumber);

    public TaskAwaiter GetAwaiter() => Execute().GetAwaiter();

    private async Task Execute()
    {
        var result = await _thenClause.GetExecutionResult();
        if (result.Result.ShouldSucceed() is not BrowseOrderHistoryVerification verification)
            throw new InvalidOperationException("Cannot verify order history: the executed operation did not browse the order history");

        foreach (var v in _verifications)
        {
            v(verification);
        }
    }
}
