using System.Runtime.CompilerServices;
using Common.Domain;
using Dsl.Port.Then.Steps;
using Dsl.Core.Shared;
using Dsl.Core.UseCase.UseCases;

namespace Dsl.Core.Scenario.Then;

public class ThenSuccessOrderHistory<TSuccessResponse, TSuccessVerification> : IThenOrderHistory
    where TSuccessVerification : ResponseVerification<TSuccessResponse>
{
    private readonly ThenStage<TSuccessResponse, TSuccessVerification> _thenClause;
    private const string AllCustomers = "All customers";

    private readonly List<Action<BrowseOrderHistoryVerification, Func<string, string>>> _verifications = [];

    internal ThenSuccessOrderHistory(ThenStage<TSuccessResponse, TSuccessVerification> thenClause)
    {
        _thenClause = thenClause;
    }

    public ThenSuccessOrderHistory<TSuccessResponse, TSuccessVerification> ContainsOrder(string orderNumber)
    {
        _verifications.Add((v, _) => v.ContainsOrder(orderNumber));
        return this;
    }

    IThenOrderHistory IThenOrderHistory.ContainsOrder(string orderNumber) => ContainsOrder(orderNumber);

    public ThenSuccessOrderHistory<TSuccessResponse, TSuccessVerification> DoesNotContainOrder(string orderNumber)
    {
        _verifications.Add((v, _) => v.DoesNotContainOrder(orderNumber));
        return this;
    }

    IThenOrderHistory IThenOrderHistory.DoesNotContainOrder(string orderNumber) => DoesNotContainOrder(orderNumber);

    public ThenSuccessOrderHistory<TSuccessResponse, TSuccessVerification> IsEmpty()
    {
        _verifications.Add((v, _) => v.IsEmpty());
        return this;
    }

    IThenOrderHistory IThenOrderHistory.IsEmpty() => IsEmpty();

    public ThenSuccessOrderHistory<TSuccessResponse, TSuccessVerification> HasTotalElements(int expectedTotalElements)
    {
        _verifications.Add((v, _) => v.TotalElements(expectedTotalElements));
        return this;
    }

    IThenOrderHistory IThenOrderHistory.HasTotalElements(int expectedTotalElements) => HasTotalElements(expectedTotalElements);

    public ThenSuccessOrderHistory<TSuccessResponse, TSuccessVerification> ShowsOrder(string orderNumber, string customerAlias,
        string skuAlias, int expectedQuantity, OrderStatus expectedStatus)
    {
        _verifications.Add((v, customerUsername) =>
            v.ShowsOrder(orderNumber, customerUsername(customerAlias), skuAlias, expectedQuantity, expectedStatus));
        return this;
    }

    IThenOrderHistory IThenOrderHistory.ShowsOrder(string orderNumber, string customerAlias, string skuAlias,
        int expectedQuantity, OrderStatus expectedStatus)
        => ShowsOrder(orderNumber, customerAlias, skuAlias, expectedQuantity, expectedStatus);

    public ThenSuccessOrderHistory<TSuccessResponse, TSuccessVerification> LinksOrderToItsDetails(string orderNumber)
    {
        _verifications.Add((v, _) => v.LinksOrderToItsDetails(orderNumber));
        return this;
    }

    IThenOrderHistory IThenOrderHistory.LinksOrderToItsDetails(string orderNumber) => LinksOrderToItsDetails(orderNumber);

    public ThenSuccessOrderHistory<TSuccessResponse, TSuccessVerification> OffersCustomers(params string[] customerAliases)
    {
        _verifications.Add((v, customerUsername) => v.OffersCustomers(customerAliases.Select(customerUsername).ToList()));
        return this;
    }

    IThenOrderHistory IThenOrderHistory.OffersCustomers(params string[] customerAliases) => OffersCustomers(customerAliases);

    public ThenSuccessOrderHistory<TSuccessResponse, TSuccessVerification> HasCustomerFilterSetToAllCustomers()
    {
        _verifications.Add((v, _) => v.CustomerFilterSetTo(AllCustomers));
        return this;
    }

    IThenOrderHistory IThenOrderHistory.HasCustomerFilterSetToAllCustomers() => HasCustomerFilterSetToAllCustomers();

    public ThenSuccessOrderHistory<TSuccessResponse, TSuccessVerification> OffersNoCustomerFilter()
    {
        _verifications.Add((v, _) => v.NoCustomerFilter());
        return this;
    }

    IThenOrderHistory IThenOrderHistory.OffersNoCustomerFilter() => OffersNoCustomerFilter();

    public ThenSuccessOrderHistory<TSuccessResponse, TSuccessVerification> ShowsNoCustomers()
    {
        _verifications.Add((v, _) => v.NoCustomerColumn());
        return this;
    }

    IThenOrderHistory IThenOrderHistory.ShowsNoCustomers() => ShowsNoCustomers();

    public ThenSuccessOrderHistory<TSuccessResponse, TSuccessVerification> ShowsMessage(string expectedMessage)
    {
        _verifications.Add((v, _) => v.Message(expectedMessage));
        return this;
    }

    IThenOrderHistory IThenOrderHistory.ShowsMessage(string expectedMessage) => ShowsMessage(expectedMessage);

    public TaskAwaiter GetAwaiter() => Execute().GetAwaiter();

    private async Task Execute()
    {
        var result = await _thenClause.GetExecutionResult();
        if (result.Result.ShouldSucceed() is not BrowseOrderHistoryVerification verification)
            throw new InvalidOperationException("Cannot verify order history: the executed operation did not browse the order history");

        foreach (var v in _verifications)
        {
            v(verification, result.CustomerUsername);
        }
    }
}
