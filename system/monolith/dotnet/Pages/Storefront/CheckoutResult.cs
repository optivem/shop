using System.Text.Json;

namespace MyCompany.MyShop.Monolith.Pages.Storefront;

/// <summary>A cart line that became an order.</summary>
public sealed record PlacedLine(string Sku, int Quantity, string OrderNumber);

/// <summary>A cart line the backend rejected, with its reason.</summary>
public sealed record RejectedLine(string Sku, int Quantity, string Reason);

/// <summary>
/// The outcome of a checkout: the order number of each placed line and the backend's reason for each rejected one.
/// Kept in the session for the confirmation page, so a reload still shows it.
/// </summary>
public sealed record CheckoutResult(IReadOnlyList<PlacedLine> Placed, IReadOnlyList<RejectedLine> Rejected)
{
    internal const string SessionKey = "myshop.lastCheckout";

    public IEnumerable<string> PlacedSkus => Placed.Select(line => line.Sku);

    public void Save(ISession session) => session.SetString(SessionKey, JsonSerializer.Serialize(this));

    public static CheckoutResult? Load(ISession session)
    {
        var json = session.GetString(SessionKey);
        return json == null ? null : JsonSerializer.Deserialize<CheckoutResult>(json);
    }
}
