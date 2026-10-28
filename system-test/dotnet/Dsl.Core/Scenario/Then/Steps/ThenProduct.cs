using Dsl.Core.External.Erp.UseCases;
using Dsl.Port.Then.Steps;

namespace Dsl.Core.Scenario.Then.Steps;

public class ThenProduct : BaseThenLookup<GetProductVerification>, IThenProduct
{
    private readonly string _skuAlias;

    public ThenProduct(UseCaseDsl app, string skuAlias, Func<Task> prelude)
        : base(app, prelude)
    {
        _skuAlias = skuAlias;
    }

    protected override async Task<GetProductVerification> Fetch()
    {
        return (await _app.Erp().GetProduct().Sku(_skuAlias).Execute()).ShouldSucceed();
    }

    public IThenProduct HasSku(string sku)
    {
        AddVerification(v => v.Sku(sku));
        return this;
    }

    public IThenProduct HasPrice(decimal price)
    {
        AddVerification(v => v.Price(price));
        return this;
    }
}
