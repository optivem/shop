using System.Text.Json;

namespace MyCompany.MyShop.Monolith.Pages.Storefront;

/// <summary>One cart line: a SKU and its quantity.</summary>
public sealed record CartLine(string Sku, int Quantity);

/// <summary>
/// The visitor's cart (UC-008), kept in the server session (storefront-ux §6.3). It holds only SKUs and quantities,
/// in the order the products were first added; prices are re-read from the catalog on display. The session cookie
/// is independent of the sign-in cookie, so the cart survives sign-in. Reading never creates a session; saving does.
/// </summary>
public sealed class Cart
{
    internal const string SessionKey = "myshop.cart";

    private readonly List<CartLine> _lines;

    private Cart(List<CartLine> lines) => _lines = lines;

    public static Cart Empty() => new([]);

    /// <summary>The session's cart: an empty cart when there is none yet.</summary>
    public static Cart Load(ISession session)
    {
        var json = session.GetString(SessionKey);
        var lines = json == null ? null : JsonSerializer.Deserialize<List<CartLine>>(json);
        return new Cart(lines ?? []);
    }

    public void Save(ISession session) => session.SetString(SessionKey, JsonSerializer.Serialize(_lines));

    public IReadOnlyList<CartLine> Lines => _lines;

    public bool IsEmpty => _lines.Count == 0;

    public int TotalQuantity => _lines.Sum(line => line.Quantity);

    /// <summary>Adds to a line, or starts one; a line never goes past the maximum quantity.</summary>
    public void Add(string sku, int quantity)
    {
        var index = _lines.FindIndex(line => line.Sku == sku);
        if (index < 0)
        {
            _lines.Add(new CartLine(sku, Quantity.Clamp(quantity)));
        }
        else
        {
            _lines[index] = _lines[index] with { Quantity = Quantity.Clamp(_lines[index].Quantity + quantity) };
        }
    }

    public void SetQuantity(string sku, int quantity)
    {
        var index = _lines.FindIndex(line => line.Sku == sku);
        if (index >= 0)
        {
            _lines[index] = _lines[index] with { Quantity = Quantity.Clamp(quantity) };
        }
    }

    public void Remove(string sku) => _lines.RemoveAll(line => line.Sku == sku);

    public void RemoveAll(IEnumerable<string> skus)
    {
        var removed = skus.ToHashSet();
        _lines.RemoveAll(line => removed.Contains(line.Sku));
    }
}
