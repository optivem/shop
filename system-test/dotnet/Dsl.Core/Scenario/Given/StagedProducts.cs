namespace Dsl.Core.Scenario.Given;

/// <summary>The ERP product lists the scenario staged, so a When step can change them after the cart was filled.</summary>
public sealed class StagedProducts
{
    public static readonly StagedProducts None = new(
        (_, _) => throw new InvalidOperationException("No ERP product list was staged, so no price can change"),
        _ => throw new InvalidOperationException("No ERP product list was staged, so no product can be withdrawn"));

    private readonly Func<string, string, Task> _changePrice;
    private readonly Func<string, Task> _withdraw;

    public StagedProducts(Func<string, string, Task> changePrice, Func<string, Task> withdraw)
    {
        _changePrice = changePrice;
        _withdraw = withdraw;
    }

    /// <summary>Stages the product lists again, with this product at the new price.</summary>
    public Task ChangePriceAsync(string skuAlias, string unitPrice) => _changePrice(skuAlias, unitPrice);

    /// <summary>The ERP no longer offers this product: the lists are staged again without it, and asking for it finds nothing.</summary>
    public Task WithdrawAsync(string skuAlias) => _withdraw(skuAlias);
}
