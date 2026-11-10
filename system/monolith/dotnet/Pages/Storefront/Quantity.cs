using System.Globalization;
using System.Text.RegularExpressions;

namespace MyCompany.MyShop.Monolith.Pages.Storefront;

/// <summary>Quantity rules shared by the product page and the cart (UC-007 BR-2, UC-008 BR-3).</summary>
public static partial class Quantity
{
    public const int Min = 1;
    public const int Max = 99;
    public const string Error = "Quantity must be a whole number from 1 to 99.";

    /// <summary>The typed quantity, or null when it is not a whole number from 1 to 99.</summary>
    public static int? Parse(string? value)
    {
        var trimmed = value?.Trim() ?? string.Empty;
        if (!Digits().IsMatch(trimmed))
        {
            return null;
        }

        var quantity = int.Parse(trimmed, CultureInfo.InvariantCulture);
        return quantity is >= Min and <= Max ? quantity : null;
    }

    public static int Clamp(int quantity) => Math.Clamp(quantity, Min, Max);

    [GeneratedRegex("^[0-9]{1,9}$")]
    private static partial Regex Digits();
}
