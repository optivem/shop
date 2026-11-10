namespace MyCompany.MyShop.Monolith.Pages.Storefront;

/// <summary>Where "Sign in" and "Add to cart" send the visitor back to.</summary>
public static class ReturnUrls
{
    /// <summary>Only same-site relative paths are accepted as destinations (no open redirect); anything else is "/".</summary>
    public static string Safe(string? value) =>
        value != null && value.StartsWith('/') && !value.StartsWith("//", StringComparison.Ordinal)
            && !value.StartsWith("/\\", StringComparison.Ordinal)
            ? value
            : "/";
}
