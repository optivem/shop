using System.Runtime.CompilerServices;
using Dsl.Port.Then.Steps;
using Dsl.Core.Shared;
using Dsl.Core.UseCase.UseCases;

namespace Dsl.Core.Scenario.Then;

public class ThenSuccessCustomerList<TSuccessResponse, TSuccessVerification> : IThenCustomerList
    where TSuccessVerification : ResponseVerification<TSuccessResponse>
{
    private readonly ThenStage<TSuccessResponse, TSuccessVerification> _thenClause;
    private readonly List<Action<BrowseCustomersVerification, Func<string, string>>> _verifications = [];

    internal ThenSuccessCustomerList(ThenStage<TSuccessResponse, TSuccessVerification> thenClause)
    {
        _thenClause = thenClause;
    }

    public ThenSuccessCustomerList<TSuccessResponse, TSuccessVerification> ListsCustomersInOrder(params string[] customerAliases)
    {
        _verifications.Add((v, customerUsername) => v.ListsCustomersInOrder(customerAliases.Select(customerUsername).ToList()));
        return this;
    }

    IThenCustomerList IThenCustomerList.ListsCustomersInOrder(params string[] customerAliases) => ListsCustomersInOrder(customerAliases);

    public TaskAwaiter GetAwaiter() => Execute().GetAwaiter();

    private async Task Execute()
    {
        var result = await _thenClause.GetExecutionResult();
        if (result.Result.ShouldSucceed() is not BrowseCustomersVerification verification)
            throw new InvalidOperationException("Cannot verify the customer list: the executed operation did not browse the customers");

        foreach (var v in _verifications)
        {
            v(verification, result.CustomerUsername);
        }
    }
}
