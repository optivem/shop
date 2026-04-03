using Dsl.Port.Then.Steps;
using Dsl.Core.Shared;

namespace Dsl.Core.Scenario.Then;

public class ThenSuccessAnd<TSuccessResponse, TSuccessVerification>
    : BaseThenAnd<TSuccessResponse, TSuccessVerification, ThenSuccessOrder<TSuccessResponse, TSuccessVerification>>, IThenSuccessAnd
    where TSuccessVerification : ResponseVerification<TSuccessResponse>
{
    internal ThenSuccessAnd(ThenStage<TSuccessResponse, TSuccessVerification> thenClause)
        : base(thenClause)
    {
    }

    protected override ThenSuccessOrder<TSuccessResponse, TSuccessVerification> CreateOrderAssertion(Func<Task<string>> orderNumberFactory)
    {
        return new ThenSuccessOrder<TSuccessResponse, TSuccessVerification>(_thenClause, orderNumberFactory);
    }

    IThenOrder IThenSuccessAnd.Order(string orderNumber) => Order(orderNumber);

    IThenOrder IThenSuccessAnd.Order() => Order();

    public async Task<IThenClock> Clock()
    {
        await _thenClause.GetExecutionResult();
        var verification = (await _thenClause.App.Clock().GetTime().Execute()).ShouldSucceed();
        return new Steps.ThenClock(_thenClause.App, verification);
    }

    async Task<IThenClock> IThenSuccessAnd.Clock() => await Clock();

    public async Task<IThenProduct> Product(string skuAlias)
    {
        await _thenClause.GetExecutionResult();
        var verification = (await _thenClause.App.Erp().GetProduct().Sku(skuAlias).Execute()).ShouldSucceed();
        return new Steps.ThenProduct(_thenClause.App, verification);
    }

    async Task<IThenProduct> IThenSuccessAnd.Product(string skuAlias) => await Product(skuAlias);
}

