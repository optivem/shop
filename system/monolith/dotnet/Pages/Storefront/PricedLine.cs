using MyCompany.MyShop.Monolith.Core.Dtos;

namespace MyCompany.MyShop.Monolith.Pages.Storefront;

/// <summary>One cart line at the current catalog price; <see cref="Product"/> is null when the catalog no longer offers the SKU.</summary>
public sealed record PricedLine(string Sku, int Quantity, ViewProductDetailsResponse? Product, decimal LineTotal)
{
    /// <summary>Prices every line of the cart against the catalog.</summary>
    public static IReadOnlyList<PricedLine> OfAll(Cart cart, IEnumerable<ViewProductDetailsResponse> products)
    {
        var bySku = new Dictionary<string, ViewProductDetailsResponse>();
        foreach (var product in products)
        {
            bySku.TryAdd(product.Sku, product);
        }

        return cart.Lines.Select(line =>
        {
            var product = bySku.GetValueOrDefault(line.Sku);
            var lineTotal = product == null ? 0m : Money.LineTotal(product.Price, line.Quantity);
            return new PricedLine(line.Sku, line.Quantity, product, lineTotal);
        }).ToList();
    }

    public int Decreased => Storefront.Quantity.Clamp(Quantity - 1);

    public int Increased => Storefront.Quantity.Clamp(Quantity + 1);
}
