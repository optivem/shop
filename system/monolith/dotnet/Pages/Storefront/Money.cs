using System.Globalization;

namespace MyCompany.MyShop.Monolith.Pages.Storefront;

/// <summary>Money display: one currency for every shipping country, with thousands separators and 2 decimals.</summary>
public static class Money
{
    /// <summary>1299.99 -> "$1,299.99".</summary>
    public static string Format(decimal amount) =>
        "$" + Math.Round(amount, 2, MidpointRounding.AwayFromZero).ToString("#,##0.00", CultureInfo.InvariantCulture);

    /// <summary>unit price x quantity.</summary>
    public static decimal LineTotal(decimal unitPrice, int quantity) => unitPrice * quantity;
}
