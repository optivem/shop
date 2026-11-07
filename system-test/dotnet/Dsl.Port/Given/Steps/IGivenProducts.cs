using Dsl.Port.Given.Steps.Base;

namespace Dsl.Port.Given.Steps;

/// <summary>
/// The ERP's enumerable product list. Unlike <see cref="IGivenProduct"/> (a single product looked up by SKU),
/// this stages the whole list the ERP returns; with no products added, the list is empty.
/// </summary>
public interface IGivenProducts : IGivenStep
{
    IGivenProducts WithProduct(string sku, string? unitPrice);

    IGivenProducts WithProduct(string sku, decimal unitPrice);
}
