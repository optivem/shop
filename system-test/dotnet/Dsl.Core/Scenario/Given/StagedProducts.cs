namespace Dsl.Core.Scenario.Given;

/// <summary>The ERP product lists the scenario staged, so a When step can change them after the cart was filled.</summary>
public sealed class StagedProducts
{
    public static readonly StagedProducts None = new((_, _) =>
        throw new InvalidOperationException("No ERP product list was staged, so no price can change"));

    private readonly Func<string, string, Task> _changePrice;

    public StagedProducts(Func<string, string, Task> changePrice)
    {
        _changePrice = changePrice;
    }

    /// <summary>Stages the product lists again, with this product at the new price.</summary>
    public Task ChangePriceAsync(string skuAlias, string unitPrice) => _changePrice(skuAlias, unitPrice);
}
