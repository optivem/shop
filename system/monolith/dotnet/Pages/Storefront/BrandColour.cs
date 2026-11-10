namespace MyCompany.MyShop.Monolith.Pages.Storefront;

/// <summary>The tile colour of a brand: the same brand always gets the same colour (storefront-ux §5).</summary>
public static class BrandColour
{
    // Dark enough for white text at >= 4.5:1 contrast; the same palette and hash as the other storefronts.
    private static readonly string[] TileColours =
        ["#1f4e9c", "#7a2e8e", "#0f6b5c", "#9c3d1f", "#3b4a5a", "#8a1c3f", "#2f5d1e", "#5b3a99"];

    public static string Of(string? brand)
    {
        long hash = 0;
        foreach (var rune in (brand ?? string.Empty).EnumerateRunes())
        {
            hash = (hash * 31 + rune.Value) % 2147483647L;
        }

        return TileColours[(int)(hash % TileColours.Length)];
    }
}
