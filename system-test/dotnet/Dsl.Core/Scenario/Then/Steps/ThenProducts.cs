using Dsl.Core.External.Erp.UseCases;
using Dsl.Port.Then.Steps;

namespace Dsl.Core.Scenario.Then.Steps;

public class ThenProducts : BaseThenLookup<GetProductsVerification>, IThenProducts
{
    public ThenProducts(UseCaseDsl app, Func<Task> prelude)
        : base(app, prelude)
    {
    }

    protected override async Task<GetProductsVerification> Fetch()
    {
        return (await _app.Erp().GetProducts().Execute()).ShouldSucceed();
    }

    public IThenProducts ContainsProduct(string skuAlias, decimal price)
    {
        AddVerification(v => v.ContainsProduct(skuAlias, price));
        return this;
    }

    public IThenProducts ContainsProduct(string skuAlias, string price)
    {
        AddVerification(v => v.ContainsProduct(skuAlias, price));
        return this;
    }
}
